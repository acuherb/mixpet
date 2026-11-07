package com.mix.common;

import com.mix.model.entity.LabOrderItem;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检验结果判定的唯一入口（服务端）：异常与危急值不再由前端正则决定。
 * 优先用开单时快照下来的数值化区间；历史单据只有 ref_range 文本时退回兼容解析。
 */
public final class LabResultEvaluator {
    private LabResultEvaluator() {}

    private static final Pattern RANGE = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*[-~～至]\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");
    private static final Pattern CMP = Pattern.compile("^\\s*([<>≤≥]=?)\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");
    private static final Pattern NUM = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    public record Verdict(boolean abnormal, boolean critical) {}

    /** 只解析结果值里的第一个数字；"阴性"/"+" 等非数值结果保留人工判定 */
    public static BigDecimal parseValue(String raw) {
        if (raw == null) return null;
        Matcher m = NUM.matcher(raw);
        return m.find() ? new BigDecimal(m.group()) : null;
    }

    public static Verdict evaluate(LabOrderItem item) {
        BigDecimal v = parseValue(item.getResultValue());
        if (v == null) return new Verdict(Boolean.TRUE.equals(item.getIsAbnormal()), false);
        // 危急值优先：超出危急阈值一律标异常 + 危急
        if (item.getCriticalLow() != null && v.compareTo(item.getCriticalLow()) < 0)
            return new Verdict(true, true);
        if (item.getCriticalHigh() != null && v.compareTo(item.getCriticalHigh()) > 0)
            return new Verdict(true, true);
        BigDecimal low = item.getRefLow();
        BigDecimal high = item.getRefHigh();
        if (low != null || high != null) {
            boolean out = (low != null && v.compareTo(low) < 0) || (high != null && v.compareTo(high) > 0);
            return new Verdict(out, false);
        }
        // 兼容旧单据：ref_range 是 "6.0-17.0" / ">=5" / "<3.5" 这类文本
        return new Verdict(matchLegacy(item.getRefRange(), v), false);
    }

    private static boolean matchLegacy(String refRange, BigDecimal v) {
        if (refRange == null || refRange.isBlank()) return false;
        Matcher range = RANGE.matcher(refRange);
        if (range.matches()) {
            return v.compareTo(new BigDecimal(range.group(1))) < 0
                    || v.compareTo(new BigDecimal(range.group(2))) > 0;
        }
        Matcher cmp = CMP.matcher(refRange);
        if (cmp.matches()) {
            BigDecimal limit = new BigDecimal(cmp.group(2));
            String op = cmp.group(1);
            return switch (op) {
                case ">", ">=" -> v.compareTo(limit) < 0;
                case "<", "<=" -> v.compareTo(limit) > 0;
                case "≥" -> v.compareTo(limit) < 0;
                case "≤" -> v.compareTo(limit) > 0;
                default -> false;
            };
        }
        return false;
    }
}
