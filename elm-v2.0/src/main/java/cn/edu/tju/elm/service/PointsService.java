package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.elm.constant.ChannelType;
import cn.edu.tju.elm.constant.PointsRecordType;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.*;
import cn.edu.tju.elm.repository.*;
import cn.edu.tju.elm.utils.EntityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PointsService {
    private final PointsAccountRepository pointsAccountRepository;
    private final PointsRuleRepository pointsRuleRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final PointsBatchRepository pointsBatchRepository;
    private final UserRepository userRepository;

    public PointsService(
            PointsAccountRepository pointsAccountRepository,
            PointsRuleRepository pointsRuleRepository,
            PointsRecordRepository pointsRecordRepository,
            PointsBatchRepository pointsBatchRepository,
            UserRepository userRepository) {
        this.pointsAccountRepository = pointsAccountRepository;
        this.pointsRuleRepository = pointsRuleRepository;
        this.pointsRecordRepository = pointsRecordRepository;
        this.pointsBatchRepository = pointsBatchRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取或创建用户积分账户
     */
    private PointsAccount getOrCreateAccount(Long userId) {
        Optional<PointsAccount> accountOpt = pointsAccountRepository.findByUserId(userId);
        if (accountOpt.isPresent()) {
            PointsAccount account = accountOpt.get();
            if (account.getDeleted() == null || !account.getDeleted()) {
                return account;
            }
        }
        // 账户不存在，创建新账户
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        PointsAccount account = PointsAccount.createNewAccount(user);
        return pointsAccountRepository.save(account);
    }

    /**
     * 获取用户积分账户
     */
    @Transactional(readOnly = true)
    public PointsAccount getPointsAccount(Long userId) throws PointsException {
        PointsAccount account = getOrCreateAccount(userId);
        if (account == null) {
            throw new PointsException(PointsException.ACCOUNT_NOT_FOUND);
        }
        return EntityUtils.filterEntity(account);
    }

    /**
     * 分页查询积分明细
     */
    @Transactional(readOnly = true)
    public Page<PointsRecord> getPointsRecords(Long userId, int page, int size, String type) {
        Pageable pageable = PageRequest.of(page - 1, size);
        if (type != null && !type.isEmpty() && PointsRecordType.isValidType(type)) {
            return pointsRecordRepository.findByUserIdAndType(userId, type, pageable);
        }
        return pointsRecordRepository.findByUserId(userId, pageable);
    }

    /**
     * 创建积分规则
     */
    public PointsRule createPointsRule(PointsRule rule) {
        EntityUtils.setNewEntity(rule);
        return pointsRuleRepository.save(rule);
    }

    /**
     * 获取所有积分规则
     */
    @Transactional(readOnly = true)
    public List<PointsRule> getAllPointsRules() {
        return EntityUtils.filterEntityList(pointsRuleRepository.findAll());
    }

    /**
     * 更新积分规则
     */
    public PointsRule updatePointsRule(Long id, PointsRule rule) throws PointsException {
        Optional<PointsRule> existingOpt = pointsRuleRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new PointsException(PointsException.RULE_NOT_FOUND);
        }
        PointsRule existing = existingOpt.get();
        if (existing.getDeleted() != null && existing.getDeleted()) {
            throw new PointsException(PointsException.RULE_NOT_FOUND);
        }

        existing.setChannelType(rule.getChannelType());
        existing.setRatio(rule.getRatio());
        existing.setExpireDays(rule.getExpireDays());
        existing.setDescription(rule.getDescription());
        existing.setIsEnabled(rule.getIsEnabled());
        existing.setUpdater(rule.getUpdater());
        EntityUtils.updateEntity(existing);
        return pointsRuleRepository.save(existing);
    }

    /**
     * 删除积分规则
     */
    public void deletePointsRule(Long id) throws PointsException {
        Optional<PointsRule> ruleOpt = pointsRuleRepository.findById(id);
        if (ruleOpt.isEmpty()) {
            throw new PointsException(PointsException.RULE_NOT_FOUND);
        }
        PointsRule rule = ruleOpt.get();
        EntityUtils.deleteEntity(rule);
        pointsRuleRepository.save(rule);
    }

    /**
     * 冻结积分（FIFO原则）
     */
    public Map<String, Object> freezePoints(Long userId, Integer points, String tempOrderId) throws PointsException {
        PointsAccount account = getOrCreateAccount(userId);
        if (account == null) {
            throw new PointsException(PointsException.ACCOUNT_NOT_FOUND);
        }

        // 检查可用积分是否足够
        if (account.getAvailablePoints() < points) {
            throw new PointsException(PointsException.INSUFFICIENT_POINTS);
        }

        // 按过期时间FIFO原则获取可用批次
        List<PointsBatch> availableBatches = pointsBatchRepository.findAvailableBatchesByUserIdOrderByExpireTime(userId);

        int remainingPoints = points;
        for (PointsBatch batch : availableBatches) {
            if (remainingPoints <= 0) break;

            int freezeAmount = Math.min(remainingPoints, batch.getAvailablePoints());
            batch.freezePoints(freezeAmount);
            batch.setTempOrderId(tempOrderId);
            EntityUtils.updateEntity(batch);
            pointsBatchRepository.save(batch);

            remainingPoints -= freezeAmount;
        }

        if (remainingPoints > 0) {
            throw new PointsException(PointsException.INSUFFICIENT_POINTS);
        }

        // 更新账户冻结积分
        account.freezePoints(points);
        EntityUtils.updateEntity(account);
        pointsAccountRepository.save(account);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("pointsUsed", points);
        result.put("moneySaved", points / 100); // 假设100积分=1元
        result.put("balanceSnap", account.getAvailablePoints());
        result.put("message", "锁定成功");
        return result;
    }

    /**
     * 扣除积分
     */
    public boolean deductPoints(Long userId, String tempOrderId, String finalOrderId) throws PointsException {
        PointsAccount account = getOrCreateAccount(userId);
        if (account == null) {
            throw new PointsException(PointsException.ACCOUNT_NOT_FOUND);
        }

        // 查找该临时订单号对应的冻结批次
        List<PointsBatch> frozenBatches = pointsBatchRepository.findFrozenBatchesByUserIdAndTempOrderId(userId, tempOrderId);
        if (frozenBatches.isEmpty()) {
            throw new PointsException(PointsException.DEDUCT_FAILED);
        }

        int totalDeducted = 0;
        for (PointsBatch batch : frozenBatches) {
            int frozenAmount = batch.getFrozenPoints();
            if (frozenAmount > 0) {
                batch.deductPoints(frozenAmount);
                batch.setTempOrderId(null);
                EntityUtils.updateEntity(batch);
                pointsBatchRepository.save(batch);
                totalDeducted += frozenAmount;
            }
        }

        // 更新账户
        account.deductPoints(totalDeducted);
        EntityUtils.updateEntity(account);
        pointsAccountRepository.save(account);

        // 创建消费记录
        User user = account.getUser();
        PointsRecord record = PointsRecord.createRecord(
                user,
                PointsRecordType.CONSUME,
                totalDeducted,
                finalOrderId,
                ChannelType.ORDER,
                "积分抵扣订单：" + finalOrderId
        );
        pointsRecordRepository.save(record);

        return true;
    }

    /**
     * 回滚积分
     */
    public boolean rollbackPoints(Long userId, String tempOrderId, String reason) throws PointsException {
        PointsAccount account = getOrCreateAccount(userId);
        if (account == null) {
            throw new PointsException(PointsException.ACCOUNT_NOT_FOUND);
        }

        // 查找该临时订单号对应的冻结批次
        List<PointsBatch> frozenBatches = pointsBatchRepository.findFrozenBatchesByUserIdAndTempOrderId(userId, tempOrderId);
        if (frozenBatches.isEmpty()) {
            throw new PointsException(PointsException.ROLLBACK_FAILED);
        }

        int totalRollback = 0;
        for (PointsBatch batch : frozenBatches) {
            int frozenAmount = batch.getFrozenPoints();
            if (frozenAmount > 0) {
                batch.unfreezePoints(frozenAmount);
                batch.setTempOrderId(null);
                EntityUtils.updateEntity(batch);
                pointsBatchRepository.save(batch);
                totalRollback += frozenAmount;
            }
        }

        // 更新账户
        account.unfreezePoints(totalRollback);
        EntityUtils.updateEntity(account);
        pointsAccountRepository.save(account);

        return true;
    }

    /**
     * 订单完成通知（发放积分）
     */
    public Integer notifyOrderSuccess(Long userId, String bizId, Double amount, String eventTime, String extraInfo) throws PointsException {
        // 查找ORDER渠道的启用规则
        List<PointsRule> rules = pointsRuleRepository.findByChannelTypeAndIsEnabled(ChannelType.ORDER, true);
        if (rules.isEmpty()) {
            return 0; // 没有规则，不发放积分
        }

        // 使用第一个启用的规则（可以后续扩展为选择最佳规则）
        PointsRule rule = rules.get(0);

        // 计算积分：金额 * 比例
        int points = (int) Math.round(amount * rule.getRatio());

        if (points <= 0) {
            return 0;
        }

        // 获取或创建账户
        PointsAccount account = getOrCreateAccount(userId);
        if (account == null) {
            return 0;
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                account.getUser(),
                PointsRecordType.EARN,
                points,
                bizId,
                ChannelType.ORDER,
                rule.getDescription() + " - 订单：" + bizId
        );
        pointsRecordRepository.save(record);

        // 计算过期时间
        LocalDateTime expireTime = null;
        if (rule.getExpireDays() != null && rule.getExpireDays() > 0) {
            expireTime = LocalDateTime.now().plusDays(rule.getExpireDays());
        }

        // 创建积分批次
        PointsBatch batch = PointsBatch.createBatch(account.getUser(), points, expireTime, record);
        pointsBatchRepository.save(batch);

        // 更新账户
        account.addPoints(points);
        EntityUtils.updateEntity(account);
        pointsAccountRepository.save(account);

        return points;
    }

    /**
     * 评价完成通知（发放积分）
     */
    public Integer notifyReviewSuccess(Long userId, String bizId, Integer amount, String eventTime, String extraInfo) throws PointsException {
        // 查找COMMENT渠道的启用规则
        List<PointsRule> rules = pointsRuleRepository.findByChannelTypeAndIsEnabled(ChannelType.COMMENT, true);
        if (rules.isEmpty()) {
            return 0; // 没有规则，不发放积分
        }

        // 使用第一个启用的规则
        PointsRule rule = rules.get(0);

        // 评价积分通常固定，但如果规则有比例，也可以计算
        int points = amount != null && amount > 0 ? amount : (int) Math.round(rule.getRatio());

        if (points <= 0) {
            return 0;
        }

        // 获取或创建账户
        PointsAccount account = getOrCreateAccount(userId);
        if (account == null) {
            return 0;
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                account.getUser(),
                PointsRecordType.EARN,
                points,
                bizId,
                ChannelType.COMMENT,
                rule.getDescription() + " - 评价：" + bizId
        );
        pointsRecordRepository.save(record);

        // 计算过期时间
        LocalDateTime expireTime = null;
        if (rule.getExpireDays() != null && rule.getExpireDays() > 0) {
            expireTime = LocalDateTime.now().plusDays(rule.getExpireDays());
        }

        // 创建积分批次
        PointsBatch batch = PointsBatch.createBatch(account.getUser(), points, expireTime, record);
        pointsBatchRepository.save(batch);

        // 更新账户
        account.addPoints(points);
        EntityUtils.updateEntity(account);
        pointsAccountRepository.save(account);

        return points;
    }
}
