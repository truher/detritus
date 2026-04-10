package parking_lot;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import org.team100.foreign.Lib;

/**
 * Demonstrates use of mangled names and knowledge of class internals.
 * 
 * Pro: No "extern" glue file needed.
 * Con: Brittleness, private details exposed.
 * 
 * On balance, this is a bad idea.
 * 
 * See [name mangling](https://en.wikipedia.org/wiki/Name_mangling).
 * 
 * The C++ mangled names for the Rot2 constructor are:
 * 
 * ```
 * _ZN5gtsam4Rot2C1Ed
 * _ZN5gtsam4Rot2C2Ed
 * ```
 * 
 * The mangling scheme is pretty simple:
 * 
 * start with _Z
 * "N" means "name"
 * length of next identifier ("gtsam")
 * more of those
 * "C1" means "complete constructor"
 * "C2" means "base constructor"
 * "E" means "end of the name"
 * "d" means the argument is a double
 */
public class Rot2Private {

    /**
     * A C++ constructor is the function you call after allocation, with a hidden
     * "this" pointer argument.
     */
    private static final MethodHandle ctor = Lib.linker.downcallHandle(
            Lib.lib.findOrThrow("_ZN5gtsam4Rot2C1Ed"),
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_DOUBLE));

    private static final MethodHandle theta = Lib.linker.downcallHandle(
            Lib.lib.findOrThrow("_ZNK5gtsam4Rot25thetaEv"),
            FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));
    /**
     * Mirror the C++ member layout.
     */
    private static final StructLayout layout = MemoryLayout.structLayout(
            JAVA_DOUBLE.withName("c"),
            JAVA_DOUBLE.withName("s"));
    private static final VarHandle cHandle = layout.varHandle(
            PathElement.groupElement("c"));
    private static final VarHandle sHandle = layout.varHandle(
            PathElement.groupElement("s"));

    final MemorySegment ptr;

    public Rot2Private(double theta) throws Throwable {
        // Allocate.
        ptr = Lib.arena.allocate(layout);
        // Initialize by calling the constructor.
        ctor.invokeExact(ptr, theta);
    }

    /** allocates in java */
    public Rot2Private(double c, double s) throws Throwable {
        // Allocate.
        ptr = Lib.arena.allocate(layout);
        // Initialize without calling the constructor.
        cHandle.set(ptr, 0L, c);
        sHandle.set(ptr, 0L, s);
    }

    public double theta() throws Throwable {
        return (double) theta.invokeExact(ptr);
    }

    public double c() {
        return (double) cHandle.get(ptr, 0L);
    }

    public double s() {
        return (double) sHandle.get(ptr, 0L);
    }

}
