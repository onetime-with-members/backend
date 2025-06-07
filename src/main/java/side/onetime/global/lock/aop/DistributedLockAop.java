package side.onetime.global.lock.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TokenErrorStatus;
import side.onetime.global.lock.annotation.DistributedLock;
import side.onetime.global.lock.util.CustomSpringELParser;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAop {

    private final RedissonClient redissonClient;
    private final CustomSpringELParser parser = new CustomSpringELParser();

    @Around("@annotation(lock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock lock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String dynamicKey = parser.getDynamicValue(signature.getMethod(), joinPoint.getArgs(), lock.key());
        String lockName = lock.prefix() + ":" + dynamicKey;

        RLock rLock = redissonClient.getLock(lockName);
        boolean available = false;

        try {
            available = rLock.tryLock(lock.waitTime(), lock.leaseTime(), lock.timeUnit());
            if (!available) {
                throw new CustomException(TokenErrorStatus._TOO_MANY_REQUESTS);
            }

            log.debug("🔐 락 획득: {}", lockName);
            return joinPoint.proceed();
        } finally {
            if (available && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.debug("🔓 락 해제: {}", lockName);
            }
        }
    }
}
