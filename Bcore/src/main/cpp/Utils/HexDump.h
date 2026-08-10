#ifndef SPEED_HEXDUMP_H
#define SPEED_HEXDUMP_H

namespace blackbox {

class HexDump {
public:
    static void dump(char *buf, int len, int addr);
};

} // namespace blackbox

#endif // SPEED_HEXDUMP_H
