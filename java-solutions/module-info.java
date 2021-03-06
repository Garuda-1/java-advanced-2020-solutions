module ru.ifmo.rain.dolzhanskii {
    requires info.kgeorgiy.java.advanced.walk;
    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;

    requires java.compiler;
    requires java.rmi;
    requires org.junit.jupiter.api;
    requires org.junit.platform.commons;
    requires org.junit.platform.launcher;
    requires java.desktop;

    opens ru.ifmo.rain.dolzhanskii.implementor;
    exports ru.ifmo.rain.dolzhanskii.implementor;

    exports ru.ifmo.rain.dolzhanskii.bank.demos;
    exports ru.ifmo.rain.dolzhanskii.bank.source;
    opens ru.ifmo.rain.dolzhanskii.bank.demos to java.rmi;
    opens ru.ifmo.rain.dolzhanskii.bank.source to java.rmi;

    exports ru.ifmo.rain.dolzhanskii.bank.test;
    opens ru.ifmo.rain.dolzhanskii.bank.test to org.junit.jupiter.api, org.junit.platform.commons;
}