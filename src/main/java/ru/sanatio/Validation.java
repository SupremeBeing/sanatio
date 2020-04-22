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

import ru.reflexio.IInstanceFieldReflection;
import ru.reflexio.IInstanceMethodReflection;
import ru.reflexio.IReflection;
import ru.reflexio.ITypeReflection;
import ru.reflexio.MetaAnnotation;
import ru.reflexio.TypeReflection;
import ru.sanatio.conversion.IStringConverter;
import ru.sanatio.validator.IValidator;
import ru.sanatio.validator.ValidatorReference;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Validation {

    private static final String DEFAULT_MESSAGE = "Value %s is not valid";

    private final IStringConverter converter;

    public Validation(IStringConverter converter) {
        this.converter = converter;
    }

    public ValidationResult validate(Object instance) {
        if (instance == null) {
            throw new IllegalArgumentException();
        }
        ValidationResult result = new ValidationResult();
        ITypeReflection<?> typeReflection = new TypeReflection<>(instance.getClass());
        for (IInstanceFieldReflection fieldReflection : typeReflection.getInstanceFields()) {
            Object value = fieldReflection.getValue(instance);
            validate(value, fieldReflection, result);
        }
        for (IInstanceMethodReflection methodReflection : typeReflection.getInstanceMethods()) {
            if (methodReflection.isGetter()) {
                Object value = methodReflection.invoke(instance);
                validate(value, methodReflection, result);
            }
        }
        return result;
    }

    public ValidationResult validate(Method method, Object[] args) {
        ValidationResult result = new ValidationResult();
        Parameter[] params = method.getParameters();
        for (int i = 0; i < args.length; i++) {
            validate(args[i], params[i], result);
        }
        return result;
    }

    public ValidationResult validate(Object value, AnnotatedElement element) {
        ValidationResult result = new ValidationResult();
        validate(value, element, result);
        return result;
    }

    public ValidationResult validate(Object value, IReflection reflection) {
        ValidationResult result = new ValidationResult();
        validate(value, reflection, result);
        return result;
    }

    public void validate(Object value, AnnotatedElement element, ValidationResult result) {
        for (Annotation annotation : element.getAnnotations()) {
            ValidatorReference meta = annotation.annotationType().getAnnotation(ValidatorReference.class);
            if (meta != null) {
                validate(value, meta, annotation, result);
            }
        }
    }

    public void validate(Object value, IReflection reflection, ValidationResult result) {
        for (MetaAnnotation<ValidatorReference> vr : reflection.getMetaAnnotations(ValidatorReference.class)) {
            validate(value, vr.getMetaAnnotation(), vr.getAnnotation(), result);
        }
    }

    public void validate(Object value, ValidatorReference meta, Annotation annotation, ValidationResult result) {
        Class<? extends IValidator<? extends Annotation>> cl = meta.value();
        TypeReflection<? extends IValidator<? extends Annotation>> tr = new TypeReflection<>(cl);
        @SuppressWarnings("unchecked")
        IValidator<Annotation> validator = (IValidator<Annotation>) tr.instantiate();
        boolean valid = validator.validate(value, annotation);
        if (!valid) {
            result.addMessage(getValidationMessage(value, annotation, converter));
        }
    }

    private String getValidationMessage(Object value, Annotation annotation, IStringConverter converter) {
        List<Object> params = new ArrayList<>();
        params.add(value);
        String message = DEFAULT_MESSAGE;
        ITypeReflection<?> tr = new TypeReflection<>(annotation.annotationType());
        for (IInstanceMethodReflection method : tr.getInstanceMethods()) {
            Object methodValue = method.invoke(annotation);
            if ("message".equals(method.getName())) {
                message = methodValue.toString();
            } else {
                params.add(methodValue);
            }
        }
        return String.format(converter == null ? message : converter.getString(message), params.toArray());
    }

}
