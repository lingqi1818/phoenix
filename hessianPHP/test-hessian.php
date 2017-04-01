<?php
include_once 'HessianPHP/src/HessianClient.php';
$testurl = 'http://127.0.0.1:9898/com.fangcloud.phoenix.test.HelloService';
$proxy = new HessianClient($testurl);
for ($x=0; $x<=10000; $x++) {
$start = microtime(true);
$result = $proxy->hello($x);
$elapsed = microtime(true) - $start;
//echo  $elapsed*1000;
echo $result->{"sex"};
echo "\r\n";
//echo $result->{"name"};
}
echo "\r\n";
?>
