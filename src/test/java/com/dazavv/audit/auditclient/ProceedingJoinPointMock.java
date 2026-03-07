package com.dazavv.audit.auditclient;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;

import java.lang.reflect.Method;

public class ProceedingJoinPointMock implements ProceedingJoinPoint {

    private final Object target;
    private final Method method;
    private final Object[] args;

    public ProceedingJoinPointMock(Object target, Method method, Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
    }

    @Override
    public void set$AroundClosure(AroundClosure aroundClosure) {

    }

    @Override
    public void stack$AroundClosure(AroundClosure arc) {
        ProceedingJoinPoint.super.stack$AroundClosure(arc);
    }

    @Override
    public Object proceed() throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @Override
    public Object proceed(Object[] objects) throws Throwable {
        try {
            return method.invoke(target, objects);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @Override
    public String toShortString() {
        return "";
    }

    @Override
    public String toLongString() {
        return "";
    }

    @Override public Object getThis() { return target; }
    @Override public Object getTarget() { return target; }
    @Override public Object[] getArgs() { return args; }
    @Override public Signature getSignature() { return new SignatureMock(method); }
    @Override public SourceLocation getSourceLocation() { return null; }
    @Override public String getKind() { return null; }
    @Override public StaticPart getStaticPart() { return null; }

    static class SignatureMock implements Signature {
        private final Method method;
        public SignatureMock(Method method) { this.method = method; }
        @Override public String getName() { return method.getName(); }
        @Override public int getModifiers() { return method.getModifiers(); }
        @Override public Class getDeclaringType() { return method.getDeclaringClass(); }
        @Override public String getDeclaringTypeName() { return method.getDeclaringClass().getName(); }
        @Override public String toShortString() { return method.getName(); }
        @Override public String toLongString() { return method.toString(); }
    }
}
