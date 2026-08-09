#pragma once

#include <jni.h>
#include <android/log.h>
#include <sys/mman.h>
#include <unistd.h>
#include <string>
#include <vector>
#include <sstream>
#include <iomanip>

#define KITTY_TAG "KittyMemory"

namespace KittyMemory {

    struct ProcMap {
        uintptr_t addressStart;
        uintptr_t addressEnd;
        long length;
        int protection;
        std::string pathname;

        bool isValid() const {
            return addressStart != 0 && addressEnd != 0;
        }
    };

    class Memory {
    public:
        static ProcMap getLibraryMap(const char *libraryName, pid_t pid = getpid()) {
            ProcMap map = {0, 0, 0, 0, ""};
            char filename[32];
            snprintf(filename, sizeof(filename), "/proc/%d/maps", pid);
            FILE *fp = fopen(filename, "r");
            if (!fp) return map;

            char line[512];
            while (fgets(line, sizeof(line), fp)) {
                if (strstr(line, libraryName)) {
                    uintptr_t start, end;
                    char perms[5];
                    char path[256];
                    if (sscanf(line, "%lx-%lx %4s %*s %*s %*s %s", &start, &end, perms, path) >= 3) {
                        if (perms[0] == 'r') { // executable/readable segment
                            map.addressStart = start;
                            map.addressEnd = end;
                            map.length = end - start;
                            map.pathname = path;
                            break;
                        }
                    }
                }
            }
            fclose(fp);
            return map;
        }

        static bool setProtection(uintptr_t address, size_t size, int protection) {
            uintptr_t page_size = sysconf(_SC_PAGESIZE);
            uintptr_t start = address & ~(page_size - 1);
            uintptr_t end = ((address + size + page_size - 1) & ~(page_size - 1));
            return mprotect((void *)start, end - start, protection) == 0;
        }
    };
}
