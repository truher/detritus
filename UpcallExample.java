package ffm_test;

import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.team100.foreign.Lib;

import gtsam.KeyVector;
import gtsam.SharedNoiseModel;
import gtsam.Vector;

public class UpcallExample {
    private static final Arena arena = Arena.ofShared();
    private static final SymbolLookup lib = SymbolLookup.libraryLookup("./libcpp_test.so", arena);
    private static final Linker linker = Linker.nativeLinker();

    private static final MethodHandle caller = Lib.linker.downcallHandle(
            Lib.lib.findOrThrow("caller"),
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle caller2 = Lib.linker.downcallHandle(
            Lib.lib.findOrThrow("caller2"),
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle caller3 = Lib.linker.downcallHandle(
            Lib.lib.findOrThrow("caller3"),
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle caller4 = Lib.linker.downcallHandle(
            Lib.lib.findOrThrow("caller4"),
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

    private static final MethodHandle callme = callmeHandle();

    private static final MemorySegment callmePtr = Lib.linker.upcallStub(
            callme,
            FunctionDescriptor.ofVoid(),
            arena);

    static MethodHandle callmeHandle() {
        try {
            return MethodHandles.lookup().findStatic(
                    UpcallExample.class,
                    "callme",
                    MethodType.methodType(void.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle callme2 = callme2Handle();

    static MethodHandle callme2Handle() {
        try {
            return MethodHandles.lookup().findVirtual(
                    UpcallExample.class,
                    "callme2",
                    MethodType.methodType(void.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle callme3 = callme3Handle();

    static MethodHandle callme3Handle() {
        try {
            return MethodHandles.lookup().findVirtual(
                    UpcallExample.class,
                    "callme3",
                    MethodType.methodType(MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle callme4 = callme4Handle();

    static MethodHandle callme4Handle() {
        try {
            return MethodHandles.lookup().findVirtual(
                    UpcallExample.class,
                    "callme4",
                    MethodType.methodType(
                            MemorySegment.class,
                            MemorySegment.class,
                            MemorySegment.class,
                            MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static void callme() {
        System.out.println("java static callme called");
    }

    int x = 0;

    void callme2() {
        System.out.printf("java instance callme called x = %d\n", x);
    }

    /** @return Vector* */
    MemorySegment callme3() throws Throwable {
        System.out.println("java callme3");
        Vector v = new Vector(2);
        v.set(0, 1);
        v.set(1, 2);
        return v.ptr;
    }

    /**
     * @param customFactor CustomFactor*
     * @return Vector*
     */
    MemorySegment callme4(
            MemorySegment factor,
            MemorySegment keys,
            MemorySegment errorFunction) throws Throwable {
        System.out.println("java callme4 called");
        Vector v = new Vector(2);
        v.set(0, 1);
        v.set(1, 2);
        return v.ptr;
    }

    MemorySegment errorFunction(
            MemorySegment factor,
            MemorySegment values,
            MemorySegment jacobian) throws Throwable {
        System.out.println("java errorFunction called");
        Vector v = new Vector(2);
        v.set(0, 1);
        v.set(1, 2);
        return v.ptr;
    }

    private static final MethodHandle errorFunction = errorFunctionHandle();

    static MethodHandle errorFunctionHandle() {
        try {
            return MethodHandles.lookup().findVirtual(
                    UpcallExample.class,
                    "errorFunction",
                    MethodType.methodType(
                            MemorySegment.class,
                            MemorySegment.class,
                            MemorySegment.class,
                            MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Throwable {
        caller.invokeExact(callmePtr);

        UpcallExample ue = new UpcallExample();
        MemorySegment callme2Ptr = Lib.linker.upcallStub(
                callme2.bindTo(ue),
                FunctionDescriptor.ofVoid(),
                arena);
        caller.invokeExact(callme2Ptr);
        ue.x = 1;
        caller.invokeExact(callme2Ptr);

        MemorySegment callme3Ptr = Lib.linker.upcallStub(
                callme3.bindTo(ue),
                FunctionDescriptor.of(ADDRESS),
                arena);
        caller2.invokeExact(callme3Ptr);

        MemorySegment callme4Ptr = Lib.linker.upcallStub(
                callme4.bindTo(ue),
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS),
                arena);
        caller3.invokeExact(callme4Ptr);

        SharedNoiseModel noiseModel = SharedNoiseModel.Unit(3);
        KeyVector keys = new KeyVector();
        MemorySegment errorFunctionPtr = Lib.linker.upcallStub(
                errorFunction.bindTo(ue),
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS),
                arena);

        caller4.invokeExact(noiseModel.ptr, keys.ptr, errorFunctionPtr);

    }

}
