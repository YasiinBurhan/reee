#pragma once

#include <string>
#include <cstring>
#include <vector>
#include <cstdint>

#include "KittyMemory.hpp"

enum MP_ASM_ARCH
{
    MP_ASM_ARM32 = 0,
    MP_ASM_ARM64,
    MP_ASM_x86,
    MP_ASM_x86_64,
};

namespace KittyMemory {
    class MemoryPatch
    {
    private:
        uintptr_t _address;
        size_t _size;

        std::vector<uint8_t> _orig_code;
        std::vector<uint8_t> _patch_code;

    public:
        MemoryPatch();
        ~MemoryPatch();

        static MemoryPatch createWithBytes(uintptr_t absolute_address, const void *patch_code, size_t patch_size);
        static MemoryPatch createWithHex(uintptr_t absolute_address, std::string hex);
        static MemoryPatch createWithHex(const char *libraryName, uintptr_t absolute_address, std::string hex);

    #ifndef kNO_KEYSTONE
        static MemoryPatch createWithAsm(uintptr_t absolute_address,
                                         MP_ASM_ARCH asm_arch,
                                         const std::string &asm_code,
                                         uintptr_t asm_address = 0);
    #endif

        bool isValid() const;
        size_t get_PatchSize() const;
        size_t getSize() const { return get_PatchSize(); }

        uintptr_t get_TargetAddress() const;
        uintptr_t getAddress() const { return get_TargetAddress(); }

        bool Restore();
        bool restore() { return Restore(); }

        bool Modify();
        bool modify() { return Modify(); }

        std::string get_CurrBytes() const;
        std::string get_OrigBytes() const;
        std::string get_PatchBytes() const;
    };
}
