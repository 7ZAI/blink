package com.blink.framework;

public class Person {

    private String name;

    private Integer age;

    private Integer male;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getMale() {
        return male;
    }

    public void setMale(Integer male) {
        this.male = male;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", male=" + male +
                '}';
    }
}
