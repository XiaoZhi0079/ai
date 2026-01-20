//package com.example.ai.aop;
//
//import com.example.mapper.OperateLogMapper;
//import com.example.pojo.OperateLog;
//import com.example.utils.CurrentHolder;
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//
//
//@Slf4j
//@Aspect
//public class Aspect1 {
//
//    @Around("@annotation(com.example.anno.LogOpeartion)")
//    public Object insertLogOpeartion(ProceedingJoinPoint pjp) throws Throwable {
//
//        //获取当前时间
//        Long starttime=System.currentTimeMillis();
//        //执行目标方法
//        Object result=pjp.proceed();
//        Long endtime=System.currentTimeMillis();
//        //计算花费时间
//        Long costtime=endtime-starttime;
//
//        //打印当前时间
//        log.info("当前时间是:"+LocalDateTime.now());
//        //打印花费时间
//        log.info("花费时间为:"+costtime);
//        //打印类名
//        log.info(""+pjp.getTarget().getClass().getName());
//        //打印方法名
//        log.info("方法参数为"+Arrays.toString(pjp.getArgs()));
//
//        return result;
//    }
//}
