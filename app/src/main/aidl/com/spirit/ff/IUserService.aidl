package com.spirit.ff;

interface IUserService {
    void destroy();
    String readMemory(int pid, long address, int size);
    boolean writeFloat(int pid, long address, float value);
    boolean writeInt(int pid, long address, int value);
    int getPidByPackage(String packageName);
    long getModuleBase(int pid, String moduleName);
    boolean injectTouch(float x, float y, float ex, float ey, int ms);
    boolean isProcessRunning(int pid);
}
