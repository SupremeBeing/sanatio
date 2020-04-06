/*
 * Copyright (c) 2019, Dmitriy Shchekotin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ru.sanatio;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import ru.sanatio.conversion.SameStringConverter;
import ru.sanatio.handler.ConsoleValidationHandler;
import ru.sanatio.handler.IValidationHandler;

@Aspect
public class ValidatingAspect {

    private final Validation validation = new Validation();
    private final IValidationHandler handler = new ConsoleValidationHandler(new SameStringConverter());

    @Pointcut("execution(@Validatable * *.*(..))")
    void annotatedMethod() {}

    @Pointcut("execution(* (@Validatable *).*(..))")
    void methodOfAnnotatedClass() {}

    @Around("annotatedMethod() && @annotation(methodLevelX)")
    public Object adviseAnnotatedMethods(ProceedingJoinPoint pjp, Validatable methodLevelX) throws Throwable {
        return validate(pjp, methodLevelX);
    }

    @Around("methodOfAnnotatedClass() && !annotatedMethod() && @within(classLevelX)")
    public Object adviseMethodsOfAnnotatedClass(ProceedingJoinPoint pjp, Validatable classLevelX) throws Throwable {
        return validate(pjp, classLevelX);
    }

    private Object validate(ProceedingJoinPoint pjp, Validatable validatable) throws Throwable {
        if (pjp.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Object result = pjp.proceed();
            ValidationResult vr = validation.validate(signature.getMethod(), pjp.getArgs(), result);
            for (ValidationEntry e : vr) {
                if (!e.isValid()) {
                    handler.handle(e);
                }
            }
            return result;
        }
        return pjp.proceed();
    }

}
