<?php
/**
 * Add this action handler to your api-mobile.php file on the server.
 * This endpoint fetches mechanics that are NOT booked for the given date and time.
 */

if (isset($_GET['action']) && $_GET['action'] == 'get_available_mechanics') {
    // 1. Get request parameters
    $tenantId = isset($_GET['tid']) ? intval($_GET['tid']) : 1;
    $date = isset($_GET['date']) ? $_GET['date'] : '';
    $time = isset($_GET['time']) ? $_GET['time'] : '';

    if (empty($date) || empty($time)) {
        echo json_encode(['status' => 'error', 'message' => 'Date and time are required']);
        exit;
    }

    // 2. Find mechanics who already have active appointments overlapping with the requested time.
    // NOTE: Modify table names (`appointments`, `users`, `bays`) and column names as per your actual database schema.
    // If your appointments have duration/end_time, you can modify the WHERE clause to check for overlaps:
    // e.g. AND (appointment_time <= ? AND estimated_end_time >= ?)
    
    $unavailableQuery = "
        SELECT mechanic_id 
        FROM appointments 
        WHERE tenant_id = ? 
        AND appointment_date = ? 
        AND appointment_time = ? 
        AND status IN ('PENDING', 'CONFIRMED', 'ONGOING', 'APPROVED')
    ";
    
    $stmt = $conn->prepare($unavailableQuery);
    $stmt->bind_param("iss", $tenantId, $date, $time);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $unavailableMechanics = [];
    while ($row = $result->fetch_assoc()) {
        if (!empty($row['mechanic_id'])) {
            $unavailableMechanics[] = $row['mechanic_id'];
        }
    }
    
    // 3. Fetch mechanics who are NOT in the unavailable list
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
    
    // 4. Also fetch available bays (if needed by frontend)
    $baysQuery = "SELECT bay_id, name AS bay_name FROM bays WHERE tenant_id = ?";
    $stmt3 = $conn->prepare($baysQuery);
    $stmt3->bind_param("i", $tenantId);
    $stmt3->execute();
    $baysResult = $stmt3->get_result();
    
    $bays = [];
    while ($row = $baysResult->fetch_assoc()) {
        $bays[] = $row;
    }
    
    // 5. Return JSON response matching MechanicsBaysResponse Kotlin data class
    echo json_encode([
        'status' => 'success',
        'mechanics' => $mechanics,
        'bays' => $bays
    ]);
    exit;
}
?>
