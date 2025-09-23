package cn.edu.tju.core.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class ObjectUtils {
    public static void patch(Object existObj, Object newObj) {
        Class<?> clazz = existObj.getClass();
        // getFields() 返回 Class 对象所表示的类或接口所声明的所有字段，包括公共、保护、默认（包）访问和私有字段，但不包括继承的字段。
        // getDeclaredFields() 返回Class 对象所表示的类或接口的所有可访问公共字段。
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            //获取属相名
            String attributeName = field.getName();
            //将属性名的首字母变为大写，为执行set/get方法做准备
            String methodName = attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1);
            try {
                // getDeclaredMethod("get"+methodName) 获取该类当前属性的getXXX方法（私有和公有方法）
                // getMethod("get"+methodName) 获取该类当前属性的getXXX方法（只能获取公有方法）
                Method getMethod = newObj.getClass().getMethod("get" + methodName);
                // 执行该get方法
                Object result = getMethod.invoke(newObj);
                // if (field.get(newObj) != null) {
                if (!field.getName().equals("id") && result != null) {
                    if (field.getGenericType() == java.lang.String.class) {
                        // getDeclaredMethod("set"+methodName) 获取该类当前属性的setXXX方法（私有和公有方法）
                        // getMethod("set"+methodName) 获取该类当前属性的setXXX方法（只能获取公有方法）
                        Method setMethod = clazz.getMethod("set" + methodName, String.class);
                        //执行该set方法
                        setMethod.invoke(existObj, (String) result);
                    } else if (field.getGenericType() == java.lang.Integer.class) {
                        Method setMethod = clazz.getMethod("set" + methodName, Integer.class);
                        setMethod.invoke(existObj, (Integer) result);
                    } else if (field.getGenericType() == java.lang.Long.class) {
                        Method setMethod = clazz.getMethod("set" + methodName, Long.class);
                        setMethod.invoke(existObj, (Long) result);
                    } else if (field.getGenericType() == java.lang.Boolean.class) {
                        Method setMethod = clazz.getMethod("set" + methodName, Boolean.class);
                        setMethod.invoke(existObj, (Boolean) result);
                    } else if (field.getGenericType() == java.time.LocalDateTime.class) {
                        Method setMethod = clazz.getMethod("set" + methodName, LocalDateTime.class);
                        setMethod.invoke(existObj, (LocalDateTime) result);
                    }
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
