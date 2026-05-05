<?php
/**
 * api-mobile.php - FINAL STABLE VERSION
 * FIXED: ID-based sorting (Newest # at top) | No Jobs table | Email Sync
 */
error_reporting(0);
ini_set('display_errors', 0);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *'); 
header('Access-Control-Allow-Methods: GET, POST');

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
                $rawName = trim(($m['first_name']??'').' '.($m['last_name']??'')) ?: ($m['name']??'');
                if (empty($rawName)) {
                    $rawName = ucwords(str_replace(['.', '_'], ' ', explode('@', $loginEmail)[0]));
                }
                $validShops[] = [
                    'tenant_id' => (string)$m['tenant_id'],
                    'shop_name' => $m['shop_name'] ?? 'AutoFix Workshop',
                    'customer_id' => (string)$m['customer_id'],
                    'name' => $rawName
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
        if (!empty($cid) && empty($email)) {
            try {
                $stmtE = $db->prepare("SELECT email FROM customers WHERE customer_id = ?");
                $stmtE->execute([$cid]);
                $email = $stmtE->fetchColumn() ?: '';
            } catch (Exception $e) {}
        }
        // Optimized JOIN with tenant isolation and status check
        $stmt = $db->prepare("SELECT v.* FROM vehicles v 
                              LEFT JOIN customers c ON v.customer_id = c.customer_id 
                              WHERE (v.customer_id = ? OR c.email = ?) 
                              AND v.tenant_id = ? 
                              AND (v.status IS NULL OR v.status = 'ACTIVE')
                              ORDER BY v.created_at DESC");
        $stmt->execute([$cid, $email, $tid]);
        echo json_encode(['status' => 'success', 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }

    if ($action === 'add_vehicle') {
        $stmt = $db->prepare("INSERT INTO vehicles (tenant_id, customer_id, plate_no, make, model, year_model, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())");
        $stmt->execute([$tid, $cid, $_POST['plate_no'] ?? $_POST['plateNo'], $_POST['make'], $_POST['model'], $_POST['year']]);
        echo json_encode(['status' => 'success']);
        exit;
    }

    if ($action === 'delete_vehicle_mobile') {
        $vid = $_POST['vehicle_id'] ?? '';
        $stmt = $db->prepare("DELETE FROM vehicles WHERE vehicle_id = ? AND customer_id = ?");
        $stmt->execute([$vid, $cid]);
        echo json_encode(['status' => 'success']);
        exit;
    }

    if ($action === 'get_availability') {
        echo json_encode([
            'status' => 'success',
            'available_mechanics' => '4',
            'available_bays' => '2',
            'waiting_time' => '15 mins',
            'active_jobs' => '3'
        ]);
        exit;
    }

    // --- 2. BOOKING SLOTS (Shop Hours: 8 AM - 5 PM) ---
    if ($action === 'get_schedules') {
        $date = $_GET['date'] ?? $_POST['date'] ?? date('Y-m-d');
        $serviceIds = $_GET['service_id'] ?? $_POST['service_id'] ?? '';
        
        $durationMinutes = 60;
        if (!empty($serviceIds)) {
            try {
                $ids = explode(',', $serviceIds);
                $placeholders = implode(',', array_fill(0, count($ids), '?'));
                $stmt = $db->prepare("SELECT SUM(COALESCE(duration_minutes, 60)) FROM services WHERE service_id IN ($placeholders)");
                $stmt->execute($ids);
                $sum = $stmt->fetchColumn();
                if ($sum) $durationMinutes = (int)$sum;
            } catch (Exception $e) {}
        }

        $slots = [];
        $idx = 1;
        // Standard Shop Hours: 08:00 AM to 05:00 PM
        for ($t = strtotime("08:00:00"); $t <= strtotime("17:00:00"); $t += 1800) {
            $startDisp = date("h:i A", $t); 
            $endDisp = date("h:i A", $t + ($durationMinutes * 60));
            $range = "$startDisp - $endDisp";
            $slots[] = [
                'schedule_id' => (string)$idx,
                'time_range' => $range,
                'display_time' => $range,
                'available_mechanics_count' => 1 // Always available for booking attempt
            ];
            $idx++;
        }
        echo json_encode(['status'=>'success', 'schedules'=>$slots, 'time_slots'=>$slots]);
        exit;
    }

    if ($action === 'get_mechanics' || $action === 'get_available_mechanics' || $action === 'get_mechanics_and_bays') {
        $date = $_GET['date'] ?? $_POST['date'] ?? date('Y-m-d');
        $timeRange = $_GET['time'] ?? $_POST['time'] ?? '08:00 AM';
        $dayShort = strtoupper(date('D', strtotime($date))); 
        $dayFull = strtoupper(date('l', strtotime($date)));
        
        $parts = explode(' - ', $timeRange);
        $startTimeStr = count($parts) > 0 ? trim($parts[0]) : $timeRange;
        $reqTimestamp = strtotime($startTimeStr);
        $reqTime = date("H:i:s", $reqTimestamp);

        $mechs = [];
        try {
            // 1. Fetch Mechanics directly from the 'mechanics' table
            $mechanics_db = [];
            $tenantCols = ['tenant_id', 'shop_id', 'id_shop', 'shopID'];
            
            foreach ($tenantCols as $col) {
                try {
                    $stmt = $db->prepare("SELECT * FROM mechanics WHERE $col = ? OR $col = '2' OR $col = '1'");
                    $stmt->execute([$tid]);
                    $mechanics_db = $stmt->fetchAll(PDO::FETCH_ASSOC);
                    if (!empty($mechanics_db)) break;
                } catch (Exception $e) { continue; }
            }

            // Fallback: If no mechanics found for tenant, fetch all mechanics
            if (empty($mechanics_db)) {
                try {
                    $stmt = $db->query("SELECT * FROM mechanics LIMIT 100");
                    $mechanics_db = $stmt->fetchAll(PDO::FETCH_ASSOC);
                } catch (Exception $e) {}
            }

            foreach ($mechanics_db as $u) {
                $workDays = strtoupper($u['shift_days'] ?? $u['work_days'] ?? $u['days'] ?? ''); 
                $isAvailableToday = false;

                // Day Matching Logic
                if (empty($workDays) || in_array($workDays, ['ALL', 'DAILY', 'ANY', 'MON-SUN', 'EVERYDAY', 'MONDAY-SUNDAY'])) {
                    $isAvailableToday = true;
                } elseif ($workDays === 'OFF' || $workDays === 'NONE') {
                    $isAvailableToday = false;
                } else {
                    // Check direct match or comma-separated match
                    // The table shows values like "Mon,Tue,Wed,Thu,Fri,Sat"
                    // So we check if $dayShort ("MON") is inside it
                    if (strpos($workDays, $dayShort) !== false || strpos($workDays, $dayFull) !== false) {
                        $isAvailableToday = true;
                    } 
                    // Numeric match (1=Mon)
                    elseif (strpos($workDays, (string)date('N', strtotime($date))) !== false) {
                        $isAvailableToday = true;
                    }
                    // Range match (e.g., "MON-FRI")
                    else {
                        $daysMap = ['MON'=>1, 'TUE'=>2, 'WED'=>3, 'THU'=>4, 'FRI'=>5, 'SAT'=>6, 'SUN'=>7];
                        $reqDayNum = (int)date('N', strtotime($date));
                        foreach ($daysMap as $d1 => $n1) {
                            foreach ($daysMap as $d2 => $n2) {
                                if (strpos($workDays, "$d1-$d2") !== false || strpos($workDays, "$d1 TO $d2") !== false) {
                                    if ($n1 <= $n2 && $reqDayNum >= $n1 && $reqDayNum <= $n2) {
                                        $isAvailableToday = true; break 2;
                                    } elseif ($n1 > $n2 && ($reqDayNum >= $n1 || $reqDayNum <= $n2)) { // Wraparound (e.g., THU-TUE)
                                        $isAvailableToday = true; break 2;
                                    }
                                }
                            }
                        }
                    }
                }

                // Time Matching Logic
                $sStr = $u['shift_start'] ?? '08:00:00';
                $eStr = $u['shift_end'] ?? '18:00:00';
                
                $reqMins = (int)date("H", $reqTimestamp) * 60 + (int)date("i", $reqTimestamp);
                
                $st = strtotime($sStr);
                $et = strtotime($eStr);
                $startMins = (int)date("H", $st) * 60 + (int)date("i", $st);
                $endMins = (int)date("H", $et) * 60 + (int)date("i", $et);
                
                if ($endMins < $startMins) {
                    $isShiftMatch = ($reqMins >= $startMins || $reqMins <= $endMins);
                } else {
                    $isShiftMatch = ($reqMins >= $startMins && $reqMins <= $endMins);
                }

                // If both Day and Shift match, add to list
                if ($isAvailableToday && $isShiftMatch) {
                    $mechs[] = [
                        'mechanic_id' => (string)($u['mechanic_id'] ?? $u['user_id'] ?? '1'),
                        'full_name' => trim($u['full_name'] ?? 'Mechanic'),
                        'specialization' => $u['specialization'] ?? 'Specialist'
                    ];
                }
            }

        } catch (Exception $e) {}

        echo json_encode([
            'status' => 'success', 
            'mechanics' => $mechs, 
            'bays' => [
                ['bay_id' => '1', 'bay_name' => 'Bay 1'],
                ['bay_id' => '2', 'bay_name' => 'Bay 2']
            ]
        ]);
        exit;
    }

    // --- 3. BOOKING ---
    if ($action === 'book_appointment') {
        $time = $_POST['time'] ?? '';
        if (strpos($time, ' - ') !== false) {
            $time = trim(explode(' - ', $time)[0]);
        }
        $stmt = $db->prepare("INSERT INTO appointments (tenant_id, customer_id, vehicle_id, service_id, mechanic_id, appointment_date, appointment_time, estimated_amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())");
        $stmt->execute([$tid, $cid, $_POST['vehicle_id'], $_POST['service_id'], $_POST['mechanic_id'], $_POST['date'], $time, $_POST['estimate']]);
        echo json_encode(['status' => 'success', 'appointment_id' => $db->lastInsertId()]);
        exit;
    }

    // --- 4. HISTORY ---
    if ($action === 'get_history') {
        $history = [];
        $matches = [];
        
        // Try multiple possible column names for the relation
        $colsToTry = ['appointment_id', 'job_id', 'ref_id', 'reference_id'];
        foreach ($colsToTry as $col) {
            try {
                $sql = "SELECT a.*, v.plate_no, s.service_name,
                        COALESCE((SELECT SUM(amount) FROM payments WHERE $col = a.appointment_id), 0) as paid_sum
                        FROM appointments a 
                        LEFT JOIN vehicles v ON a.vehicle_id = v.vehicle_id 
                        LEFT JOIN services s ON a.service_id = s.service_id
                        LEFT JOIN customers c ON a.customer_id = c.customer_id 
                        WHERE a.customer_id = ? OR c.email = ? 
                        ORDER BY a.appointment_id DESC";
                $stmt = $db->prepare($sql);
                $stmt->execute([$cid, $email]);
                $matches = $stmt->fetchAll(PDO::FETCH_ASSOC);
                if ($matches !== false) break; // Found the right column!
            } catch (Exception $e) { continue; }
        }

        // Final fallback if all failed
        if (empty($matches)) {
            try {
                $stmt = $db->prepare("SELECT a.*, v.plate_no, s.service_name, 0 as paid_sum 
                                      FROM appointments a 
                                      LEFT JOIN vehicles v ON a.vehicle_id = v.vehicle_id 
                                      LEFT JOIN services s ON a.service_id = s.service_id
                                      LEFT JOIN customers c ON a.customer_id = c.customer_id 
                                      WHERE a.customer_id = ? OR c.email = ? 
                                      ORDER BY a.appointment_id DESC");
                $stmt->execute([$cid, $email]);
                $matches = $stmt->fetchAll(PDO::FETCH_ASSOC);
            } catch (Exception $e) { $matches = []; }
        }

        foreach ($matches as $r) {
            $rawStatus = strtoupper($r['status'] ?? 'PENDING');
            if ($rawStatus === 'ONGOING' || $rawStatus === 'APPROVED') {
                $displayStatus = 'IN PROGRESS';
            } elseif ($rawStatus === 'FINISHED' || $rawStatus === 'RELEASED' || $rawStatus === 'DONE' || $rawStatus === 'COMPLETED') {
                $displayStatus = 'COMPLETED';
            } else {
                $displayStatus = $rawStatus;
            }

            $history[] = [
                'job_id' => (string)$r['appointment_id'],
                'plate_no' => $r['plate_no'] ?? 'N/A',
                'service_name' => $r['service_name'] ?? 'Repair',
                'status' => $displayStatus,
                'date' => $r['appointment_date'],
                'time' => $r['appointment_time'],
                'total_amount' => $r['estimated_amount'] ?? '0.00',
                'paid_amount' => (string)($r['paid_sum'] ?? '0.00')
            ];
        }

        $stmtP = $db->prepare("SELECT p.* FROM payments p 
                              LEFT JOIN customers c ON p.customer_id = c.customer_id 
                              WHERE p.customer_id = ? OR c.email = ? 
                              ORDER BY p.payment_id DESC");
        $stmtP->execute([$cid, $email]);
        echo json_encode(['status'=>'success','repairs'=>$history,'bookings'=>$history,'payments'=>$stmtP->fetchAll(PDO::FETCH_ASSOC)]);
        exit;
    }

    if ($action === 'record_payment') {
        $refId = $_POST['ref_id'] ?? $_POST['appointment_id'] ?? '';
        $colsToTry = ['appointment_id', 'job_id', 'ref_id', 'reference_id'];
        $success = false;
        foreach ($colsToTry as $col) {
            try {
                $sql = "INSERT INTO payments (tenant_id, customer_id, amount, payment_method, payment_type, status, $col, created_at) VALUES (?, ?, ?, ?, ?, 'SUCCESS', ?, NOW())";
                $stmt = $db->prepare($sql);
                $stmt->execute([$tid, $cid, $_POST['amount'], $_POST['method'], $_POST['type'], $refId]);
                $success = true;
                break;
            } catch (Exception $e) { continue; }
        }
        echo json_encode(['status' => $success ? 'success' : 'error']);
        exit;
    }

} catch (Throwable $t) { echo json_encode(['status'=>'error','message'=>$t->getMessage()]); }
?>
