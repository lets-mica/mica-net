package org.tio.utils.hutool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassUtil 单元测试
 *
 * @author L.cm
 */
class ClassUtilTest {

    /**
     * 测试 isPresent 方法
     */
    @Test
    void testIsPresent() {
        // 测试存在的类
        assertTrue(ClassUtil.isPresent("java.lang.String"));
        assertTrue(ClassUtil.isPresent("java.util.ArrayList"));
        assertTrue(ClassUtil.isPresent("org.tio.utils.hutool.ClassUtil"));

        // 测试不存在的类
        assertFalse(ClassUtil.isPresent("com.example.NonExistentClass"));
        assertFalse(ClassUtil.isPresent("invalid.class.name!@#"));
    }

    /**
     * 测试 getFieldValue 方法
     */
    @Test
    void testGetFieldValue() {
        // 测试获取公共字段
        TestClass testObj = new TestClass();
        assertEquals("publicValue", ClassUtil.getFieldValue(testObj, "publicField"));
        // 测试获取私有字段
        assertEquals("privateValue", ClassUtil.getFieldValue(testObj, "privateField"));
        // 测试获取继承的字段
        assertEquals("parentValue", ClassUtil.getFieldValue(testObj, "parentField"));
        // 测试获取父类的私有字段
        assertEquals("parentPrivateValue", ClassUtil.getFieldValue(testObj, "parentPrivateField"));
    }

    /**
     * 测试获取不存在的字段
     */
    @Test
    void testGetFieldValueNonExistentField() {
        TestClass testObj = new TestClass();
        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.getFieldValue(testObj, "nonExistentField");
        });
    }

    /**
     * 测试获取字段值的类型
     */
    @Test
    void testGetFieldValueTypes() {
        TypeTestClass obj = new TypeTestClass();

        assertEquals("stringValue", ClassUtil.getFieldValue(obj, "stringField"));
        assertEquals(42, ClassUtil.getFieldValue(obj, "intField"));
        assertEquals(Boolean.TRUE, ClassUtil.getFieldValue(obj, "booleanField"));
        assertEquals(3.14, ClassUtil.getFieldValue(obj, "doubleField"));
    }

    // 测试用的类
    static class ParentClass {
        public String parentField = "parentValue";
        private String parentPrivateField = "parentPrivateValue";
        public String getParentPrivateField() {
            return parentPrivateField;
        }
    }

    static class TestClass extends ParentClass {
        public String publicField = "publicValue";
        private String privateField = "privateValue";
    }

    static class TypeTestClass {
        public String stringField = "stringValue";
        public int intField = 42;
        public boolean booleanField = true;
        public double doubleField = 3.14;
    }
}
