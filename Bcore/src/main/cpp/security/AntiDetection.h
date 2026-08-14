#ifndef BLACKBOX_ANTIDETECTION_H
#define BLACKBOX_ANTIDETECTION_H

namespace blackbox {

class AntiDetection {
public:
    static void init();
    static void setTargetPackage(const char* pkg);
    static const char* getTargetPackage();
};

} // namespace blackbox

#endif // BLACKBOX_ANTIDETECTION_H
