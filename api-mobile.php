<?php
/**
 * api-mobile.php - COMPLETE MASTER BACKEND FOR AUTOFIX APP
 * Handles: Login, Garage, Services, Mechanics, Slots, Booking, History, Tracking, Loyalty, Chat.
 */
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *'); 
header('Access-Control-Allow-Methods: GET, POST');

error_reporting(0);
ini_set('display_errors', 0);

require_once 'db-config.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';
$tid = $_GET['tid'] ?? $_POST['tid'] ?? '1';
$cid = $_POST['customer_id'] ?? $_GET['customer_id'] ?? null;

if (!$action) {
    echo json_encode(['status' => 'error', 'message' => 'No action specified.']);
    exit;
}

try {
    $db = getDB();
    if ($db) { $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION); }

    // --- 1. LOGIN ---
    if ($action === 'login') {
        $email = $_POST['email'] ?? ''; 
        $password = $_POST['password'] ?? '';
        $mobile = $_POST['mobile'] ?? '';

        $stmt = $db->prepare("SELECT c.customer_id, c.tenant_id, c.full_name, c.email, c.password_hash, t.shop_name FROM customers c JOIN tenants t ON c.tenant_id = t.tenant_id WHERE (c.mobile = ? OR c.email = ?) AND c.status = 'ACTIVE'");
        $stmt->execute([$mobile, $email]);
        $matches = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $valid_shops = [];
        foreach ($matches as $m) {
            if (password_verify($password, $m['password_hash'])) {
                $valid_shops[] = ['tenant_id' => (string)$m['tenant_id'], 'shop_name' => $m['shop_name'], 'customer_id' => (string)$m['customer_id'], 'name' => $m['full_name']];
            }
        }
        if (count($valid_shops) > 0) {
            $f = $valid_shops[0];
            echo json_encode(['status' => 'success', 'customer_id' => $f['customer_id'], 'name' => $f['name'], 'email' => $email, 'tenant_id' => $f['tenant_id'], 'shop_name' => $f['shop_name'], 'role' => 'CUSTOMER', 'shops' => $valid_shops]);
            exit;
        }
        echo json_encode(['status' => 'error', 'message' => 'Invalid email or password.']);
        exit;
    }

    // --- 2. GARAGE ---
    if ($action === 'get_garage') {
        $stmt = $db->prepare("SELECT vehicle_id, plate_no, make, model, year_model FROM vehicles WHERE customer_id = ? ORDER BY created_at DESC");
        $stmt->execute([$cid]);
        echo json_encode(['status' => 'success', 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }
    if ($action === 'add_vehicle') {
        $stmt = $db->prepare("INSERT INTO vehicles (tenant_id, customer_id, plate_no, make, model, year_model, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())");
        $stmt->execute([$tid, $cid, $_POST['plate_no'], $_POST['make'], $_POST['model'], $_POST['year']]);
        echo json_encode(['status' => 'success', 'message' => 'Vehicle added.']);
        exit;
    }

    // --- 3. BOOKING ASSETS ---
    if ($action === 'get_services') {
        $stmt = $db->prepare("SELECT service_id, service_name, description, price FROM services WHERE tenant_id = ? AND status = 'ACTIVE'");
        $stmt->execute([$tid]);
        echo json_encode(['status' => 'success', 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }
    if ($action === 'get_mechanics') {
        $stmt = $db->prepare("SELECT user_id, name FROM users WHERE tenant_id = ? AND role_id = (SELECT role_id FROM roles WHERE role_name = 'MECHANIC' LIMIT 1) AND status = 'ACTIVE'");
        $stmt->execute([$tid]);
        echo json_encode(['status' => 'success', 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }
    if ($action === 'get_slots') {
        $date = $_GET['date'] ?? date('Y-m-d');
        $stmt = $db->prepare("SELECT count(*) as booked FROM appointments WHERE tenant_id = ? AND appointment_date = ? AND status != 'CANCELLED'");
        $stmt->execute([$tid, $date]);
        $booked = (int)$stmt->fetchColumn();
        $total_bays = 5; // Default bays
        echo json_encode(['status' => 'success', 'available_slots' => max(0, $total_bays - $booked)]);
        exit;
    }

    // --- 3.5. NEW: GET AVAILABLE MECHANICS (Filtering booked ones) ---
    if ($action === 'get_available_mechanics' || $action === 'get_mechanics_and_bays') {
        $date = $_GET['date'] ?? $_POST['date'] ?? date('Y-m-d');
        $time = $_GET['time'] ?? $_POST['time'] ?? '';

        $unavailableIds = [];
        if ($time) {
            $stmt = $db->prepare("SELECT DISTINCT mechanic_id FROM appointments WHERE tenant_id = ? AND appointment_date = ? AND appointment_time = ? AND status IN ('PENDING', 'CONFIRMED', 'ONGOING', 'APPROVED')");
            $stmt->execute([$tid, $date, $time]);
            $unavailableIds = array_filter($stmt->fetchAll(PDO::FETCH_COLUMN));
        }

        $query = "SELECT u.user_id as mechanic_id, u.name as full_name, r.role_name as specialization FROM users u JOIN roles r ON u.role_id = r.role_id WHERE u.tenant_id = ? AND r.role_name = 'MECHANIC' AND u.status = 'ACTIVE'";
        $params = [$tid];

        if (!empty($unavailableIds)) {
            $placeholders = implode(',', array_fill(0, count($unavailableIds), '?'));
            $query .= " AND u.user_id NOT IN ($placeholders)";
            $params = array_merge($params, array_values($unavailableIds));
        }

        $stmt = $db->prepare($query);
        $stmt->execute($params);
        $mechanics = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $bays = [];
        try {
            $stmtB = $db->prepare("SELECT bay_id, name as bay_name FROM bays WHERE tenant_id = ?");
            $stmtB->execute([$tid]);
            $bays = $stmtB->fetchAll(PDO::FETCH_ASSOC);
        } catch (Exception $e) {
            for ($i = 1; $i <= 5; $i++) { $bays[] = ['bay_id' => (string)$i, 'bay_name' => "Bay $i"]; }
        }

        echo json_encode(['status' => 'success', 'mechanics' => $mechanics, 'bays' => $bays]);
        exit;
    }

    // --- 4. BOOKING EXECUTION ---
    if ($action === 'book_appointment') {
        $stmt = $db->prepare("INSERT INTO appointments (tenant_id, customer_id, vehicle_id, service_id, mechanic_id, appointment_date, appointment_time, estimated_amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())");
        $stmt->execute([$tid, $cid, $_POST['vehicle_id'], $_POST['service_id'], $_POST['mechanic_id'], $_POST['date'], $_POST['time'], $_POST['estimate']]);
        echo json_encode(['status' => 'success', 'appointment_id' => $db->lastInsertId()]);
        exit;
    }

    // --- 5. HISTORY & REPAIRS ---
    if ($action === 'get_history') {
        $repairs = [];
        $stmt = $db->prepare("SELECT a.*, v.plate_no, s.service_name, u.name as mechanic_name FROM appointments a LEFT JOIN vehicles v ON a.vehicle_id = v.vehicle_id LEFT JOIN services s ON a.service_id = s.service_id LEFT JOIN users u ON a.mechanic_id = u.user_id WHERE a.customer_id = ? ORDER BY a.appointment_date DESC");
        $stmt->execute([$cid]);
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $r) {
            $repairs[] = [
                'job_id' => (string)$r['appointment_id'],
                'plate_no' => $r['plate_no'] ?? 'N/A',
                'service_name' => $r['service_name'] ?? 'General Service',
                'status' => $r['status'],
                'date' => $r['appointment_date'],
                'time' => $r['appointment_time'],
                'mechanic' => $r['mechanic_name'] ?? 'Assigned Soon',
                'total_amount' => $r['estimated_amount']
            ];
        }
        $payments = [];
        $stmtP = $db->prepare("SELECT * FROM payments WHERE customer_id = ? ORDER BY created_at DESC");
        $stmtP->execute([$cid]);
        foreach ($stmtP->fetchAll(PDO::FETCH_ASSOC) as $p) {
            $payments[] = ['payment_id' => $p['payment_id'], 'amount' => $p['amount'], 'method' => $p['payment_method'], 'status' => $p['status'], 'date' => $p['created_at']];
        }
        echo json_encode(['status' => 'success', 'repairs' => $repairs, 'payments' => $payments]);
        exit;
    }

    // --- 6. TRACKING ---
    if ($action === 'get_repair_status') {
        $job_id = $_GET['job_id'] ?? '';
        $stmt = $db->prepare("SELECT status, updated_at FROM appointments WHERE appointment_id = ?");
        $stmt->execute([$job_id]);
        $res = $stmt->fetch(PDO::FETCH_ASSOC);
        echo json_encode(['status' => 'success', 'current_status' => $res['status'] ?? 'UNKNOWN', 'last_update' => $res['updated_at'] ?? '']);
        exit;
    }

    // --- 7. LOYALTY ---
    if ($action === 'loyalty_status') {
        $stmt = $db->prepare("SELECT points FROM customers WHERE customer_id = ?");
        $stmt->execute([$cid]);
        $points = (int)$stmt->fetchColumn();
        $tier = "BRONZE"; $next = "SILVER"; $target = 500;
        if ($points >= 500) { $tier = "SILVER"; $next = "GOLD"; $target = 1500; }
        if ($points >= 1500) { $tier = "GOLD"; $next = "PLATINUM"; $target = 5000; }
        echo json_encode(['status' => 'success', 'points' => $points, 'member_level' => $tier, 'next_tier' => $next, 'points_to_next' => max(0, $target - $points)]);
        exit;
    }

    // --- 8. CHAT ---
    if ($action === 'get_messages') {
        $stmt = $db->prepare("SELECT * FROM messages WHERE (sender_id = ? OR receiver_id = ?) AND tenant_id = ? ORDER BY created_at ASC");
        $stmt->execute([$cid, $cid, $tid]);
        echo json_encode(['status' => 'success', 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }

} catch (Exception $e) {
    echo json_encode(['status' => 'error', 'message' => 'System error: ' . $e->getMessage()]);
}
?>
