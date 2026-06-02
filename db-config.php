<?php
// db-config.php - Database connection settings
// Update these with your actual database credentials

function getDB() {
    $host = 'localhost'; // Usually localhost on InfinityFree
    $dbname = 'YOUR_DB_NAME';
    $username = 'YOUR_USERNAME';
    $password = 'YOUR_PASSWORD';

    try {
        $db = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
        $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $db->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        // Set timezone for MySQL NOW() to Philippines Time
        $db->exec("SET time_zone = '+08:00'");
        return $db;
    } catch (PDOException $e) {
        // Return null or handle error
        return null;
    }
}
