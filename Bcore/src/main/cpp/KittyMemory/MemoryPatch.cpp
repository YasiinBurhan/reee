#include "MemoryPatch.hpp"
#include "KittyMemory.hpp"
#include "KittyUtils.hpp"
#include <sys/mman.h>
#include <unistd.h>
#include <sstream>
#include <cstdlib>
#include <cstring>

namespace KittyMemory {

    static bool set_prot(uintptr_t addr, size_t size, int prot) {
        uintptr_t page_size = sysconf(_SC_PAGESIZE);
        uintptr_t start = addr & ~(page_size - 1);
        uintptr_t diff = addr - start;
        return mprotect((void *)start, size + diff, prot) == 0;
    }

    MemoryPatch::MemoryPatch() : _address(0), _size(0) {}

    MemoryPatch::~MemoryPatch() {}

    MemoryPatch MemoryPatch::createWithBytes(uintptr_t absolute_address, const void *patch_code, size_t patch_size) {
        MemoryPatch mp;
        if (absolute_address == 0 || patch_code == nullptr || patch_size == 0) return mp;

        mp._address = absolute_address;
        mp._size = patch_size;
        mp._patch_code.resize(patch_size);
        std::memcpy(mp._patch_code.data(), patch_code, patch_size);

        mp._orig_code.resize(patch_size);
        if (set_prot(mp._address, mp._size, PROT_READ | PROT_WRITE | PROT_EXEC) ||
            set_prot(mp._address, mp._size, PROT_READ | PROT_WRITE)) {
            std::memcpy(mp._orig_code.data(), (void *)mp._address, patch_size);
        }
        return mp;
    }

    MemoryPatch MemoryPatch::createWithHex(uintptr_t absolute_address, std::string hex) {
        std::vector<uint8_t> bytes;
        std::stringstream ss(hex);
        std::string byte_val;
        while (ss >> byte_val) {
            bytes.push_back((uint8_t)strtoul(byte_val.c_str(), nullptr, 16));
        }
        return createWithBytes(absolute_address, bytes.data(), bytes.size());
    }

    MemoryPatch MemoryPatch::createWithHex(const char *libraryName, uintptr_t absolute_address, std::string hex) {
        auto maps = KittyMemory::getMaps(KittyMemory::EProcMapFilter::Contains, libraryName);
        if (maps.empty()) return MemoryPatch();
        return createWithHex(maps[0].startAddress + absolute_address, hex);
    }

#ifndef kNO_KEYSTONE
    MemoryPatch MemoryPatch::createWithAsm(uintptr_t absolute_address, MP_ASM_ARCH asm_arch, const std::string &asm_code, uintptr_t asm_address) {
        return MemoryPatch();
    }
#endif

    bool MemoryPatch::isValid() const {
        return _address != 0 && !_patch_code.empty();
    }

    size_t MemoryPatch::get_PatchSize() const {
        return _size;
    }

    uintptr_t MemoryPatch::get_TargetAddress() const {
        return _address;
    }

    bool MemoryPatch::Modify() {
        if (!isValid()) return false;
        if (!set_prot(_address, _size, PROT_READ | PROT_WRITE | PROT_EXEC) &&
            !set_prot(_address, _size, PROT_READ | PROT_WRITE)) {
            return false;
        }
        std::memcpy((void *)_address, _patch_code.data(), _size);
        __builtin___clear_cache((char *)_address, (char *)_address + _size);
        return true;
    }

    bool MemoryPatch::Restore() {
        if (!isValid()) return false;
        if (!set_prot(_address, _size, PROT_READ | PROT_WRITE | PROT_EXEC) &&
            !set_prot(_address, _size, PROT_READ | PROT_WRITE)) {
            return false;
        }
        std::memcpy((void *)_address, _orig_code.data(), _size);
        __builtin___clear_cache((char *)_address, (char *)_address + _size);
        return true;
    }

    std::string MemoryPatch::get_CurrBytes() const {
        if (!isValid()) return "";
        return "";
    }

    std::string MemoryPatch::get_OrigBytes() const {
        if (!isValid() || _orig_code.empty()) return "";
        return "";
    }

    std::string MemoryPatch::get_PatchBytes() const {
        if (!isValid() || _patch_code.empty()) return "";
        return "";
    }
}
