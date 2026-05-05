<?php
/**
 * api-mobile.php - FINAL STABLE VERSION
 * FIXED: ID-based sorting (Newest # at top) | No Jobs table | Email Sync
 */
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *'); 
header('Access-Control-Allow-Methods: GET, POST');

error_reporting(0);
ini_set('display_errors', 0);

set_exception_handler(function($t) {
    echo json_encode(['status'=>'error','message'=>'API Sync: '.$t->getMessage(),'repairs'=>[],'bookings'=>[],'payments'=>[]]);
    exit;
});

try {
    include 'db-config.php';
    $db = getDB();
    if (!$db) throw new Exception("Database Offline");
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $action = $_GET['action'] ?? $_POST['action'] ?? '';
    $tid = $_GET['tid'] ?? $_POST['tid'] ?? '1';
    $cid = trim($_POST['customer_id'] ?? $_GET['customer_id'] ?? '');
    $email = trim($_GET['email'] ?? $_POST['email'] ?? '');

    // --- 0. LOGIN ---
    if ($action === 'login') {
        $loginEmail = $_POST['email'] ?? ''; 
        $password = $_POST['password'] ?? '';
        $stmt = $db->prepare("SELECT c.*, t.shop_name FROM customers c JOIN tenants t ON c.tenant_id = t.tenant_id WHERE c.email = ? AND c.status = 'ACTIVE'");
        $stmt->execute([$loginEmail]);
        $matches = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $validShops = [];
        foreach ($matches as $m) {
            if (password_verify($password, $m['password_hash'])) {
                $validShops[] = [
                    'tenant_id' => (string)$m['tenant_id'],
                    'shop_name' => $m['shop_name'] ?? 'AutoFix Workshop',
                    'customer_id' => (string)$m['customer_id'],
                    'name' => trim(($m['first_name']??'').' '.($m['last_name']??'')) ?: ($m['name']??'User')
                ];
            }
        }
        if (!empty($validShops)) {
            echo json_encode(['status' => 'success', 'customer_id' => $validShops[0]['customer_id'], 'name' => $validShops[0]['name'], 'email' => $loginEmail, 'tenant_id' => $validShops[0]['tenant_id'], 'shop_name' => $validShops[0]['shop_name'], 'shops' => $validShops]);
        } else { echo json_encode(['status' => 'error', 'message' => 'Invalid credentials']); }
        exit;
    }

    // --- 1. GARAGE & SERVICES ---
    if ($action === 'get_services') {
        $stmt = $db->prepare("SELECT * FROM services WHERE tenant_id = ? AND (status IS NULL OR status = 'ACTIVE')");
        $stmt->execute([$tid]);
        echo json_encode(['status' => 'success', 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }

    if ($action === 'get_garage' || $action === 'get_vehicles') {
        $stmt = $db->prepare("SELECT v.* FROM vehicles v WHERE v.customer_id = ? OR (SELECT email FROM customers WHERE customer_id = v.customer_id) = ? ORDER BY v.created_at DESC");
        $stmt->execute([$cid, $email]);
        echo json_encode(['status' => 'success', 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }

    // --- 2. BOOKING SLOTS ---
    if ($action === 'get_schedules') {
        $slots = [];
        for ($t = strtotime("08:00:00"); $t < strtotime("17:00:00"); $t += 3600) {
            $start = date("h:i A", $t); $end = date("h:i A", $t+3600);
            $slots[] = ['schedule_id'=>(string)($t),'time_range'=>"$start - $end",'display_time'=>"$start - $end",'available_mechanics_count'=>4];
        }
        echo json_encode(['status'=>'success','schedules'=>$slots]);
        exit;
    }

    // --- 3. BOOKING ---
    if ($action === 'book_appointment') {
        $stmt = $db->prepare("INSERT INTO appointments (tenant_id, customer_id, vehicle_id, service_id, mechanic_id, appointment_date, appointment_time, estimated_amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())");
        $stmt->execute([$tid, $cid, $_POST['vehicle_id'], $_POST['service_id'], $_POST['mechanic_id'], $_POST['date'], $_POST['time'], $_POST['estimate']]);
        echo json_encode(['status' => 'success', 'appointment_id' => $db->lastInsertId()]);
        exit;
    }

    // --- 4. HISTORY (FIXED SORTING) ---
    if ($action === 'get_history') {
        $history = [];
        // SORT BY appointment_id DESC to ensure newest numbers are at the TOP
        $stmt = $db->prepare("SELECT a.*, v.plate_no, s.service_name 
                              FROM appointments a 
                              LEFT JOIN vehicles v ON a.vehicle_id = v.vehicle_id 
                              LEFT JOIN services s ON a.service_id = s.service_id
                              LEFT JOIN customers c ON a.customer_id = c.customer_id 
                              WHERE a.customer_id = ? OR c.email = ? 
                              ORDER BY a.appointment_id DESC");
        $stmt->execute([$cid, $email]);
        foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $r) {
            $rawStatus = strtoupper($r['status'] ?? 'PENDING');
            $displayStatus = ($rawStatus === 'ONGOING' || $rawStatus === 'APPROVED') ? 'IN PROGRESS' : $rawStatus;
            
            $history[] = [
                'job_id' => (string)$r['appointment_id'],
                'plate_no' => $r['plate_no'] ?? 'N/A',
                'service_name' => $r['service_name'] ?? 'Car Repair',
                'status' => $displayStatus,
                'date' => $r['appointment_date'],
                'time' => $r['appointment_time'],
                'total_amount' => $r['estimated_amount'] ?? '0.00'
            ];
        }
        // Stable retrieval of payments (we use ref_id as Job Number in the app)
        $stmtP = $db->prepare("SELECT p.* FROM payments p 
                              LEFT JOIN customers c ON p.customer_id = c.customer_id 
                              WHERE p.customer_id = ? OR c.email = ? 
                              ORDER BY p.payment_id DESC");
        $stmtP->execute([$cid, $email]);
        echo json_encode(['status'=>'success','repairs'=>$history,'bookings'=>$history,'payments'=>$stmtP->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }

    if ($action === 'record_payment') {
        $stmt = $db->prepare("INSERT INTO payments (tenant_id, customer_id, amount, payment_method, payment_type, status, ref_id, created_at) VALUES (?, ?, ?, ?, ?, 'SUCCESS', ?, NOW())");
        $stmt->execute([$tid, $cid, $_POST['amount'], $_POST['method'], $_POST['type'], $_POST['ref_id']]);
        echo json_encode(['status'=>'success']);
        exit;
    }

} catch (Throwable $t) { echo json_encode(['status'=>'error','message'=>$t->getMessage()]); }
?>
