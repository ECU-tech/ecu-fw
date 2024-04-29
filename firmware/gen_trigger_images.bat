rem This script reads triggers.txt generated by unit_test binary and produces a bunch of trigger shapes .png files
rem these images placed into unit_tests/triggers
rem and later manually published at https://rusefi.com/images/triggers/

cd ../unit_tests
make -j4
if not exist build/ecu_test.exe echo UNIT TEST COMPILATION FAILED
if not exist build/ecu_test.exe exit -1

rem This is me using Cygwin on all my Windows devices
ls -l build/ecu_test.exe
 

del triggers.txt
build\ecu_test.exe
pwd

if not exist triggers.txt echo triggers.txt generation FAILED
if not exist triggers.txt exit -1

java -cp ../java_console_binary/ecu_console.jar com.rusefi.TriggerImage