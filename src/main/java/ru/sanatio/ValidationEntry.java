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

import ru.reflexio.IInstanceMethodReflection;
import ru.reflexio.ITypeReflection;
import ru.reflexio.TypeReflection;
import ru.sanatio.conversion.IStringConverter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ValidationEntry {

    private final boolean valid;
    private final Annotation annotation;
    private final Object value;

    public ValidationEntry(Object value, Annotation annotation, boolean valid) {
        this.value = value;
        this.annotation = Objects.requireNonNull(annotation);
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public Object getValue() {
        return value;
    }

    public String getValidationMessage() {
        return getValidationMessage(null);
    }

    public String getValidationMessage(IStringConverter converter) {
        List<Object> params = new ArrayList<>();
        params.add(value);
        String message = null;
        ITypeReflection<?> tr = new TypeReflection<>(annotation.annotationType());
        for (IInstanceMethodReflection method : tr.getInstanceMethods()) {
            Object methodValue = method.invoke(annotation);
            if ("message".equals(method.getName())) {
                message = methodValue.toString();
            } else {
                params.add(methodValue);
            }
        }
        if (message != null) {
            return String.format(converter == null ? message : converter.getString(message), params.toArray());
        }
        return null;
    }

}
