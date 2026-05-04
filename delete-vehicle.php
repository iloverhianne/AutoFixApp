<?php
/**
 * delete-vehicle.php - Isolated vehicle deletion script for maximum stability.
 */
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');

require_once 'db-config.php';

// Get parameters from any source
$vid = $_REQUEST['vehicle_id'] ?? '';
$cid = $_REQUEST['customer_id'] ?? '';

if (empty($vid)) {
    echo json_encode(['status' => 'error', 'message' => 'Missing vehicle_id.']);
    exit;
}

try {
    $db = getDB();
    if (!$db) {
        throw new Exception("Database connection failed.");
    }
    
    // We use a direct DELETE here. 
    // If there are foreign key constraints, we will catch the error.
    $stmt = $db->prepare("DELETE FROM vehicles WHERE vehicle_id = ?");
    $stmt->execute([$vid]);
    
    // Check if anything was actually deleted
    if ($stmt->rowCount() > 0) {
        echo json_encode(['status' => 'success', 'message' => 'Vehicle removed from database.']);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Vehicle not found or already deleted.']);
    }

} catch (Exception $e) {
    // Return 200 OK but with error status to prevent Retrofit parsing issues
    echo json_encode([
        'status' => 'error', 
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
