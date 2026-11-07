package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.dto.FosterDto;
import com.mix.model.dto.FosterPetDto;
import com.mix.model.entity.Foster;
import com.mix.security.model.RoleConst;
import com.mix.service.IFosterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 寄养表 前端控制器
 * </p>
 *
 * @author MixJade
 * @since 2023-01-12
 */
@RestController
@RequestMapping("/foster")
public class FosterController {
    private final IFosterService fosterService;

    @Autowired
    public FosterController(IFosterService fosterService) {
        this.fosterService = fosterService;
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<FosterDto> getPage(int numPage, int pageSize, String clientName, String petName) {
        return fosterService.selectByPage(clientName, petName, numPage, pageSize);
    }

    /**
     * 用户前台：查看被寄养的宠物
     *
     * @return 被寄养宠物的卡片信息
     */
    @GetMapping("/pet")
    public IPage<FosterPetDto> fosterPet(int pageSize) {
        return fosterService.fosterPet(pageSize);
    }

    @PostMapping
    @Secured(RoleConst.NURSE)
    public Result save(@RequestBody Foster foster) {
        if (foster.getFosterMoney() <= 0) return Result.error("金额必须大于零");
        if (fosterService.haveFoster(foster.getPetId()) > 0) return Result.error("当前宠物已在寄养中");
        return Result.choice("添加", fosterService.save(foster));
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除单个", fosterService.deleteById(id));
    }

    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.MANAGER)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return Result.choice("删除多个", fosterService.deleteByIds(ids));
    }

    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody Foster foster) {
        if (foster.getFosterMoney() <= 0) return Result.error("金额必须大于零");
        return Result.choice("修改", fosterService.updateFoster(foster));
    }

    /** 接回结算：金额一律由后端按天数×单价重算，前端只传日期 */
    @PutMapping("/back/{id}")
    @Secured(RoleConst.MANAGER)
    public Result back(@PathVariable Integer id, @RequestBody java.util.Map<String, String> body) {
        LocalDate backDate = null;
        String s = body == null ? null : body.get("backDate");
        if (s != null && !s.isBlank()) {
            try {
                backDate = LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
            } catch (Exception e) {
                return Result.error("接回日期格式不正确");
            }
        }
        return fosterService.back(id, backDate);
    }
}
