#!/system/bin/sh

echo Setting DPI...
wm density 200
echo Sending Power On...
input keyevent KEYCODE_POWER
echo Resetting SN65DSI83...
echo 43 > /sys/class/gpio/export
echo out > /sys/class/gpio/gpio43/direction
echo 0 > /sys/class/gpio/gpio43/value
echo 1 > /sys/class/gpio/gpio43/value
echo SN65DSI83 ID Read...
i2cget -y 6 0x2C 0x00
i2cget -y 6 0x2C 0x01
i2cget -y 6 0x2C 0x02
i2cget -y 6 0x2C 0x03
i2cget -y 6 0x2C 0x04
i2cget -y 6 0x2C 0x05
i2cget -y 6 0x2C 0x06
i2cget -y 6 0x2C 0x07
i2cget -y 6 0x2C 0x08
echo Setup SN65DSI83...
echo Reset and Clock Registers...
#i2cset -y 6 0x2C 0x09 0x00 b
i2cset -y 6 0x2C 0x0A 0x03 b
i2cset -y 6 0x2C 0x0B 0x10 b
#i2cset -y 6 0x2C 0x0D 0x00 b
echo DSI Registers...
i2cset -y 6 0x2C 0x10 0x26 b
i2cset -y 6 0x2C 0x11 0x00 b
i2cset -y 6 0x2C 0x12 0x1e b
i2cset -y 6 0x2C 0x13 0x00 b
echo LVDS Registers...
i2cset -y 6 0x2C 0x18 0x78 b
i2cset -y 6 0x2C 0x19 0x00 b
i2cset -y 6 0x2C 0x1A 0x03 b
i2cset -y 6 0x2C 0x1B 0x00 b
echo Video Registers...
i2cset -y 6 0x2C 0x20 0x00 b
i2cset -y 6 0x2C 0x21 0x04 b
i2cset -y 6 0x2C 0x22 0x00 b
i2cset -y 6 0x2C 0x23 0x00 b
i2cset -y 6 0x2C 0x24 0x00 b
i2cset -y 6 0x2C 0x25 0x00 b
i2cset -y 6 0x2C 0x26 0x00 b
i2cset -y 6 0x2C 0x27 0x00 b
i2cset -y 6 0x2C 0x28 0x20 b
i2cset -y 6 0x2C 0x29 0x00 b
i2cset -y 6 0x2C 0x2A 0x00 b
i2cset -y 6 0x2C 0x2B 0x00 b
i2cset -y 6 0x2C 0x2C 0x6c b
i2cset -y 6 0x2C 0x2D 0x00 b
i2cset -y 6 0x2C 0x2E 0x00 b
i2cset -y 6 0x2C 0x2F 0x00 b
i2cset -y 6 0x2C 0x30 0x0c b
i2cset -y 6 0x2C 0x31 0x00 b
i2cset -y 6 0x2C 0x32 0x00 b
i2cset -y 6 0x2C 0x33 0x00 b
i2cset -y 6 0x2C 0x34 0x6c b
i2cset -y 6 0x2C 0x35 0x00 b
i2cset -y 6 0x2C 0x36 0x00 b
i2cset -y 6 0x2C 0x37 0x00 b
i2cset -y 6 0x2C 0x38 0x00 b
i2cset -y 6 0x2C 0x39 0x00 b
i2cset -y 6 0x2C 0x3A 0x00 b
i2cset -y 6 0x2C 0x3B 0x00 b
i2cset -y 6 0x2C 0x3C 0x00 b
i2cset -y 6 0x2C 0x3D 0x00 b
i2cset -y 6 0x2C 0x3E 0x00 b
echo Reset and Clock Registers...
i2cset -y 6 0x2C 0x0D 0x01 b
i2cset -y 6 0x2C 0x09 0x01 b
echo Reading status...
i2cset -y 6 0x2C 0xE5 0xFF b
i2cget -y 6 0x2C 0xE5
echo Sending Power On...
input keyevent KEYCODE_POWER
echo Enabling Always On Mode...
svc power stayon true
