#include <gtsam/base/Vector.h>
#include <gtsam/geometry/Pose2.h>
#include <gtsam/geometry/Rot2.h>
#include <gtsam/nonlinear/BatchFixedLagSmoother.h>
#include <gtsam/nonlinear/CustomFactor.h>
#include <gtsam/nonlinear/LevenbergMarquardtParams.h>

extern "C" {
/** This is analogous to the gtsam function consumer. */
void stdCaller(std::function<void()> f) {
    std::cout << "stdCaller\n";
    f();
}

// called from java with a function pointer.
void caller(void (*callme)(void)) {
    std::function<void()> fn = std::bind(callme);
    stdCaller(fn);
}

gtsam::Vector stdCaller2(std::function<gtsam::Vector()> f) {
    std::cout << "stdCaller2 \n";
    return f();
}

// the function returns a pointer
void caller2(gtsam::Vector* (*callme)(void)) {
    std::function<gtsam::Vector*()> fn = std::bind(callme);
    gtsam::Vector v = stdCaller2([fn]() { return *(fn()); });
    std::cout << "caller2: \n" << v << "\n";
}

gtsam::Vector stdCaller3(         //
    std::function<gtsam::Vector(  //
        gtsam::CustomFactor&,     //
        gtsam::KeyVector&,        //
        gtsam::CustomErrorFunction&)>
        f) {
    std::cout << "stdCaller32\n";
    gtsam::CustomFactor factor;
    gtsam::KeyVector keys;
    gtsam::CustomErrorFunction errorFunction;
    return f(factor, keys, errorFunction);
}

void caller3(gtsam::Vector* (*callme)(gtsam::CustomFactor*,  //
                                      gtsam::KeyVector*,     //
                                      gtsam::CustomErrorFunction*)) {
    std::function<gtsam::Vector*(gtsam::CustomFactor*,  //
                                 gtsam::KeyVector*,     //
                                 gtsam::CustomErrorFunction*)>
        fn = std::bind(callme,                 //
                       std::placeholders::_1,  //
                       std::placeholders::_2,  //
                       std::placeholders::_3);
    gtsam::Vector v =
        stdCaller3([fn](gtsam::CustomFactor& factor,  //
                        gtsam::KeyVector& keys,       //
                        gtsam::CustomErrorFunction& errorFunction) {
            return *(fn(&factor, &keys, &errorFunction));
        });
    std::cout << "caller3: \n" << v << "\n";
}

/** Like CustomFactor constructor, takes three references. */
void stdCaller4(const gtsam::SharedNoiseModel& noiseModel,  //
                const gtsam::KeyVector& keys,               //
                const gtsam::CustomErrorFunction& errorFunction) {
    gtsam::Vector v =
        errorFunction(gtsam::CustomFactor(), gtsam::Values(), NULL);
    std::cout << "stdCaller4: \n" << v << "\n";
}

void caller4(const gtsam::SharedNoiseModel* noiseModel,                   //
             const gtsam::KeyVector* keys,                                //
             gtsam::Vector* (*errorFunction)(const gtsam::CustomFactor*,  //
                                             const gtsam::Values*,        //
                                             const gtsam::JacobianVector*)) {
    std::cout << "caller4 start\n";
    stdCaller4(
        *noiseModel,                                        //
        *keys,                                              //
        [errorFunction](const gtsam::CustomFactor& factor,  //
                        const gtsam::Values& v,             //
                        const gtsam::JacobianVector* H) -> gtsam::Vector {
            return *(errorFunction(&factor, &v, H));
        });
    std::cout << "caller4 end\n";
}
}
