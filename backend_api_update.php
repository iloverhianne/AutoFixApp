<?php
/**
 * Add these action handlers to your api-mobile.php file on the server.
 * This covers dynamic schedule generation, availability checks, and overlap detection.
 */

// ---------------------------------------------------------
// 1. GET SCHEDULES
// ---------------------------------------------------------
if (isset($_GET['action']) && $_GET['action'] == 'get_schedules') {
    $tenantId = isset($_GET['tid']) ? intval($_GET['tid']) : 1;
    $date = isset($_GET['date']) ? $_GET['date'] : '';
    $serviceIds = isset($_GET['service_id']) ? $_GET['service_id'] : '';

    if (empty($date) || empty($serviceIds)) {
        echo json_encode(['status' => 'error', 'message' => 'Date and Service are required']);
        exit;
    }

    // A. Calculate the total duration of the selected services
    // Assume `services` table has a `duration_minutes` column. If not, fallback to 60.
    $durationMinutes = 60; 
    $serviceArray = explode(',', $serviceIds);
    if (count($serviceArray) > 0) {
        $placeholders = implode(',', array_fill(0, count($serviceArray), '?'));
        $serviceQuery = "SELECT SUM(COALESCE(duration_minutes, 60)) AS total_duration FROM services WHERE service_id IN ($placeholders) AND tenant_id = ?";
        
        $stmt = $conn->prepare($serviceQuery);
        $types = str_repeat("i", count($serviceArray)) . "i";
        $params = array_merge($serviceArray, [$tenantId]);
        $stmt->bind_param($types, ...$params);
        $stmt->execute();
        $res = $stmt->get_result();
        if ($row = $res->fetch_assoc()) {
            if (!empty($row['total_duration'])) {
                $durationMinutes = intval($row['total_duration']);
            }
        }
    }

    // B. Generate standard shop hours slots (e.g., 8:00 AM to 5:00 PM)
    $startTime = strtotime("08:00:00");
    $endTime = strtotime("17:00:00");
    $interval = 60 * 60; // 1 hour intervals
    
    $availableSchedules = [];
    $slotIdCounter = 1;
    
    for ($t = $startTime; $t < $endTime; $t += $interval) {
        $slotStart = date("h:i A", $t);
        // Calculate estimated end time based on dynamic duration
        $slotEnd = date("h:i A", $t + ($durationMinutes * 60));
        
        $timeRange = $slotStart . " - " . $slotEnd;
        
        // C. Count available mechanics for this specific slot range
        // Count total mechanics
        $totalMechsQuery = "SELECT COUNT(*) as total FROM users WHERE tenant_id = ? AND role = 'Mechanic' AND status = 'Active'";
        $stmt2 = $conn->prepare($totalMechsQuery);
        $stmt2->bind_param("i", $tenantId);
        $stmt2->execute();
        $totalMechs = $stmt2->get_result()->fetch_assoc()['total'];
        
        // Check for overlaps with duration
        $dbStart = date("H:i:s", $t);
        $dbEnd = date("H:i:s", $t + ($durationMinutes * 60));
        
        $bookedMechsQuery = "
            SELECT COUNT(DISTINCT mechanic_id) as booked
            FROM appointments 
            WHERE tenant_id = ? 
            AND appointment_date = ? 
            AND status IN ('PENDING', 'CONFIRMED', 'ONGOING', 'APPROVED')
            AND (
                (TIME(appointment_time) <= ? AND TIME(COALESCE(estimated_end_time, ADDTIME(appointment_time, '01:00:00'))) > ?)
                OR
                (TIME(appointment_time) >= ? AND TIME(appointment_time) < ?)
            )
        ";
        
        $stmt3 = $conn->prepare($bookedMechsQuery);
        $stmt3->bind_param("isssss", $tenantId, $date, $dbStart, $dbStart, $dbStart, $dbEnd);
        $stmt3->execute();
        $bookedMechs = $stmt3->get_result()->fetch_assoc()['booked'];
        
        $availableCount = $totalMechs - $bookedMechs;
        
        // D. Only return schedule if at least 1 mechanic is available
        if ($availableCount > 0) {
            $availableSchedules[] = [
                'schedule_id' => strval($slotIdCounter++),
                'time_range' => $timeRange,
                'available_mechanics_count' => $availableCount
            ];
        }
    }

    echo json_encode([
        'status' => 'success',
        'schedules' => $availableSchedules
    ]);
    exit;
}

// ---------------------------------------------------------
// 2. GET AVAILABLE MECHANICS FOR A SPECIFIC TIME RANGE
// ---------------------------------------------------------
if (isset($_GET['action']) && $_GET['action'] == 'get_available_mechanics') {
    $tenantId = isset($_GET['tid']) ? intval($_GET['tid']) : 1;
    $date = isset($_GET['date']) ? $_GET['date'] : '';
    // time will now be the time_range string (e.g. "9:00 AM - 10:00 AM")
    $timeRange = isset($_GET['time']) ? $_GET['time'] : '';

    if (empty($date) || empty($timeRange)) {
        echo json_encode(['status' => 'error', 'message' => 'Date and time are required']);
        exit;
    }

    // Extract start and end time from the range
    $parts = explode(' - ', $timeRange);
    $dbStart = date("H:i:s", strtotime($parts[0]));
    $dbEnd = date("H:i:s", strtotime($parts[1] ?? $parts[0]));

    $unavailableQuery = "
        SELECT mechanic_id 
        FROM appointments 
        WHERE tenant_id = ? 
        AND appointment_date = ? 
        AND status IN ('PENDING', 'CONFIRMED', 'ONGOING', 'APPROVED')
        AND (
            (TIME(appointment_time) <= ? AND TIME(COALESCE(estimated_end_time, ADDTIME(appointment_time, '01:00:00'))) > ?)
            OR
            (TIME(appointment_time) >= ? AND TIME(appointment_time) < ?)
        )
    ";
    
    $stmt = $conn->prepare($unavailableQuery);
    $stmt->bind_param("isssss", $tenantId, $date, $dbStart, $dbStart, $dbStart, $dbEnd);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $unavailableMechanics = [];
    while ($row = $result->fetch_assoc()) {
        if (!empty($row['mechanic_id'])) {
            $unavailableMechanics[] = $row['mechanic_id'];
        }
    }
    
    $mechanicsQuery = "
        SELECT user_id AS mechanic_id, CONCAT(first_name, ' ', last_name) AS full_name, role AS specialization 
        FROM users 
        WHERE tenant_id = ? AND role = 'Mechanic' AND status = 'Active'
    ";
    
    if (count($unavailableMechanics) > 0) {
        $placeholders = implode(',', array_fill(0, count($unavailableMechanics), '?'));
        $mechanicsQuery .= " AND user_id NOT IN ($placeholders)";
    }
    
    $stmt2 = $conn->prepare($mechanicsQuery);
    if (count($unavailableMechanics) > 0) {
        $types = "i" . str_repeat("i", count($unavailableMechanics));
        $params = array_merge([$tenantId], $unavailableMechanics);
        $stmt2->bind_param($types, ...$params);
    } else {
        $stmt2->bind_param("i", $tenantId);
    }
    
    $stmt2->execute();
    $mechResult = $stmt2->get_result();
    
    $mechanics = [];
    while ($row = $mechResult->fetch_assoc()) {
        $mechanics[] = $row;
    }
    
    $baysQuery = "SELECT bay_id, name AS bay_name FROM bays WHERE tenant_id = ?";
    $stmt3 = $conn->prepare($baysQuery);
    $stmt3->bind_param("i", $tenantId);
    $stmt3->execute();
    $baysResult = $stmt3->get_result();
    
    $bays = [];
    while ($row = $baysResult->fetch_assoc()) {
        $bays[] = $row;
    }
    
    echo json_encode([
        'status' => 'success',
        'mechanics' => $mechanics,
        'bays' => $bays
    ]);
    exit;
}

// NOTE: To ensure the Home screen "Wait Time" displays the same schedule format,
// update your 'get_availability' endpoint to calculate the next available schedule
// format (e.g. "9:00 AM - 10:00 AM") and return it as the 'waiting_time' value.
?>
