package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.AdoptDto;
import com.ship.model.entity.Adopt;
import com.ship.model.entity.Client;
import com.ship.model.vo.AdoptVo;
import com.ship.security.model.RoleConst;
import com.ship.service.IAdoptService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 领养宠物订单 前端控制器
 * </p>
 *
 * @author MixJade
 * @since 2023-01-12
 */
@RestController
@RequestMapping("/adopt")
public class AdoptController {
    private final IAdoptService adoptService;

    @Autowired
    public AdoptController(IAdoptService adoptService) {
        this.adoptService = adoptService;
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<AdoptDto> getPage(int numPage, int pageSize, String clientName, String petName) {
        return adoptService.selectByPage(clientName, petName, numPage, pageSize);
    }

    /**
     * 用户：确认当前宠物是否有待审核订单
     * <p>网页读取get参数，必须用包装类，因为可能不是数字</p>
     *
     * @param petId 宠物ID
     * @return 待领养的订单数量
     */
    @GetMapping("/inAdopt")
    public int sureAdopt(Integer petId) {
        if (UserUtil.getUser() instanceof Client client) {
            Integer clientId = client.getClientId();
            return adoptService.sureInAdopt(petId, clientId);
        } else return 1;
    }

    /**
     * 管理员添加领养订单
     * <p>POST读取请求体，需要加上RequestBody注解</p>
     * <p>因为请求体不一定带json名称</p>
     *
     * @param adopt 待添加的领养信息
     * @return 添加成功
     */
    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody Adopt adopt) {
        if (adopt.getAdoptMoney() == null || adopt.getAdoptMoney() <= 0)
            return Result.error("金额必须大于零");
        return Result.choice("添加", adoptService.save(adopt));
    }

    /**
     * 用户提交领养申请
     */
    @PostMapping("/adoptPet")
    public Result adoptPet(@RequestBody Adopt adopt) {
        if (adopt.getAdoptMoney() <= 0) return Result.error("金额必须大于零");
        if (UserUtil.getUser() instanceof Client client) {
            adopt.setClientId(client.getClientId());
            return Result.choice("申请", adoptService.save(adopt));
        } else return Result.error("用户暂时未登录");
    }

    /**
     * 删除单个
     * <p>路径参数的读取需要加上PathVariable注解</p>
     * <p>不然访问到了接口也读取不了路径中的占位符</p>
     *
     * @param id 待删除的ID
     * @return 删除成功
     */
    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除单个", adoptService.deleteById(id));
    }

    /**
     * 删除多个
     * <p>这里的参数使用逗号分割</p>
     * <p>其实就是前端数组的表现形式</p>
     * <p>因此前端只需要传数组即可，注意不能传空数组</p>
     *
     * @param ids 多个待删除的ID
     * @return 删除成功
     */
    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.MANAGER)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return Result.choice("删除多个", adoptService.deleteByIds(ids));
    }

    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody Adopt adopt) {
        if (adopt.getAdoptMoney() <= 0) return Result.error("金额必须大于零");
        // 精确更新，只改金额和简介，不动 in_adopt，避免审核状态被覆盖
        return Result.choice("修改", adoptService.lambdaUpdate()
                .eq(Adopt::getAdoptId, adopt.getAdoptId())
                .set(Adopt::getAdoptMoney, adopt.getAdoptMoney())
                .set(Adopt::getAdoptInfo, adopt.getAdoptInfo())
                .update());
    }

    /**
     * 撤回已通过的领养：宠物变无主，单子变待审核
     */
    @PutMapping("/undo/{id}")
    @Secured(RoleConst.MANAGER)
    public Result undo(@PathVariable Integer id) {
        return adoptService.undoAdopt(id);
    }

    /**
     * 顾客退养：领养人把自己名下的宠物退回，宠物变无主，单子标记为已退养
     */
    @PutMapping("/returnPet/{petId}")
    public Result returnPet(@PathVariable Integer petId) {
        if (UserUtil.getUser() instanceof Client client) {
            return adoptService.returnPet(petId, client.getClientId());
        } else return Result.error("请先登录顾客账号");
    }

    /**
     * 重启被拒绝的领养单：状态改回待审核
     */
    @PutMapping("/restart/{id}")
    @Secured(RoleConst.MANAGER)
    public Result restart(@PathVariable Integer id) {
        return adoptService.restartAdopt(id);
    }

    /**
     * 管理员审核订单
     */
    @PutMapping("/adopt")
    @Secured(RoleConst.MANAGER)
    public Result toAdopt(@RequestBody AdoptVo adoptVo) {
        return adoptService.adoptAdopt(adoptVo);
    }
}
