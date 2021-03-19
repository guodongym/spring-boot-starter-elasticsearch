package com.ksyun.elastic.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

/**
 * Created by IntelliJ IDEA.
 *
 * @author zhaogd
 * Date: 2017/9/11
 */
@Aspect
public class TimeKeepingAspect {

    private static Logger logger = LoggerFactory.getLogger(TimeKeepingAspect.class);

    /**
     * 切点
     */
    @Pointcut(value = "within(com.ksyun.elastic.api.EsTemplate)")
    public void methodPointcut() {
    }

    /**
     * 记录方法处理时间
     */
    @Around("methodPointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {

        StopWatch sw = new StopWatch(pjp.getSignature().toString());
        sw.start();

        Object result;
        try {
            result = pjp.proceed();
        } finally {
            sw.stop();
            logger.info(sw.shortSummary());
        }
        return result;
    }
}
