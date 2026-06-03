<?php
include 'db-config.php';
$db = getDB();

echo "Checking 'users' table:\n";
$stmt = $db->query("SELECT * FROM users WHERE full_name LIKE '%Zhem%' OR first_name LIKE '%Zhem%' OR last_name LIKE '%Zhem%'");
$users = $stmt->fetchAll(PDO::FETCH_ASSOC);
print_r($users);

echo "Checking 'mechanics' table:\n";
try {
    $stmt = $db->query("SELECT * FROM mechanics WHERE full_name LIKE '%Zhem%' OR first_name LIKE '%Zhem%' OR last_name LIKE '%Zhem%'");
    $mechanics = $stmt->fetchAll(PDO::FETCH_ASSOC);
    print_r($mechanics);
} catch (Exception $e) {
    echo "No mechanics table or error: " . $e->getMessage() . "\n";
}
?>
