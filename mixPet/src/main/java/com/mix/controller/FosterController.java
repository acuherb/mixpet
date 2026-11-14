package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.dto.FosterDto;
import com.mix.model.dto.FosterPetDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Foster;
import com.mix.model.entity.FosterLog;
import com.mix.model.entity.FosterPhoto;
import com.mix.model.vo.FosterBackVo;
import com.mix.model.vo.FosterLogVo;
import com.mix.security.model.RoleConst;
import com.mix.service.IFosterService;
import com.mix.util.UserUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 寄养闭环：登记 → 入托 → 每日照护记录/照片 → 接回结算 → 账单与通知。
 * 顾客只读自己 client_id 名下的寄养单/日志/照片（归属校验统一在 Service）。
 */
@RestController
@RequestMapping("/foster")
public class FosterController {
    private static final Set<String> ALLOWED_IMG = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    private final IFosterService fosterService;
    private final String reportPath;

    public FosterController(IFosterService fosterService,
                            @Value("${pet-mix.report-path}") String reportPath) {
        this.fosterService = fosterService;
        this.reportPath = reportPath;
    }

    // ── 员工端 ────────────────────────────────────────────
    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public IPage<FosterDto> getPage(int numPage, int pageSize, String clientName, String petName) {
        return fosterService.selectByPage(clientName, petName, numPage, pageSize);
    }

    /** 前台匿名展示墙：脱敏（不含寄养人姓名），只展示待入托/寄养中 */
    @GetMapping("/pet")
    public IPage<FosterPetDto> fosterPet(int pageSize) {
        return fosterService.fosterPet(pageSize);
    }

    @PostMapping
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result save(@RequestBody Foster foster) {
        if (foster.getFosterMoney() == null || foster.getFosterMoney() <= 0) return Result.error("押金必须大于零");
        if (fosterService.haveFoster(foster.getPetId()) > 0) return Result.error("当前宠物已有未完成的寄养单");
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
        if (foster.getFosterMoney() == null || foster.getFosterMoney() <= 0) return Result.error("押金必须大于零");
        return Result.choice("修改", fosterService.updateFoster(foster));
    }

    /** 待入托 → 寄养中（独立接口，护士也能操作，不复用限 MANAGER 的编辑接口） */
    @PutMapping("/start/{id}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result start(@PathVariable Integer id) {
        return fosterService.start(id);
    }

    /** 接回结算：金额可人工改价（须带说明留痕），押金冲抵后净额入账 */
    @PutMapping("/back/{id}")
    @Secured(RoleConst.MANAGER)
    public Result back(@PathVariable Integer id, @RequestBody(required = false) FosterBackVo vo) {
        return fosterService.back(id, vo);
    }

    /** 员工查看某单详情（打印结算单用） */
    @GetMapping("/one/{id}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.DOCTOR})
    public FosterDto one(@PathVariable Integer id) {
        return fosterService.staffOne(id);
    }

    // ── 每日照护记录 / 照片 ───────────────────────────────
    /** 员工新增一条照护记录（文字 + 已上传好的照片文件名） */
    @PostMapping("/log")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result addLog(@RequestBody FosterLogVo vo) {
        String recorder = UserUtil.getUser() instanceof com.mix.model.entity.Doctor d ? d.getDoctorName() : null;
        return fosterService.addLog(vo, recorder);
    }

    /** 照片上传：存到 reportPath/foster/，返回文件名，由 addLog 一并落库 */
    @PostMapping("/photo/upload")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result uploadPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("请选择图片");
        String origin = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = origin.lastIndexOf('.');
        String suffix = dot >= 0 ? origin.substring(dot).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_IMG.contains(suffix)) return Result.error("仅支持 jpg/png/gif/bmp/webp 图片");
        String name = UUID.randomUUID().toString().replace("-", "") + suffix;
        try {
            File dir = new File(reportPath, "foster");
            if (!dir.exists() && !dir.mkdirs()) return Result.error("照片目录创建失败");
            file.transferTo(new File(dir, name));
            return Result.data(name);
        } catch (Exception e) {
            return Result.error("照片上传失败");
        }
    }

    /** 日志/照片列表：员工传 clientId=null 不校验；顾客由 Service 按归属过滤 */
    @GetMapping("/log/{fosterId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.CLIENT})
    public List<FosterLog> logs(@PathVariable Integer fosterId) {
        return fosterService.logsOf(fosterId, clientIdOrNull());
    }

    @GetMapping("/photo/{fosterId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.CLIENT})
    public List<FosterPhoto> photos(@PathVariable Integer fosterId) {
        return fosterService.photosOf(fosterId, clientIdOrNull());
    }

    /** 照片文件读取：先按 photoId 查归属，越权/不存在返回 404，不做匿名放行 */
    @GetMapping("/photo/file/{photoId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.CLIENT})
    public void photoFile(@PathVariable Integer photoId, HttpServletResponse response) {
        FosterPhoto p = fosterService.photoForRead(photoId, clientIdOrNull());
        File f = p == null ? null : new File(new File(reportPath, "foster"), p.getFileName());
        if (f == null || !f.exists() || !f.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String mime = MediaTypeFactory.getMediaType(f.getName())
                .map(MediaType::toString).orElse("application/octet-stream");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentType(mime);
        try (FileInputStream fis = new FileInputStream(f); OutputStream os = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) os.write(buf, 0, len);
            os.flush();
        } catch (Exception ignored) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ── 顾客端「我的寄养」 ────────────────────────────────
    @GetMapping("/self")
    @Secured(RoleConst.CLIENT)
    public List<FosterDto> self() {
        return fosterService.selfList(clientIdOrNull());
    }

    @GetMapping("/self/{id}")
    @Secured(RoleConst.CLIENT)
    public FosterDto selfOne(@PathVariable Integer id) {
        return fosterService.selfOne(id, clientIdOrNull());
    }

    /** 登录顾客才返回其 clientId，员工返回 null（表示不按归属过滤） */
    private Integer clientIdOrNull() {
        return UserUtil.getUser() instanceof Client c ? c.getClientId() : null;
    }
}
