import {App as Vpp, Component} from 'vue'
// ElementPlus：全量按需注册（白名单滞后曾导致 el-switch/el-checkbox/el-card 等渲染成无样式原生标签）
import {
    ElAlert,
    ElAvatar,
    ElBadge,
    ElButton,
    ElButtonGroup,
    ElCard,
    ElCheckbox,
    ElCheckboxGroup,
    ElCol,
    ElColorPicker,
    ElDatePicker,
    ElDescriptions,
    ElDescriptionsItem,
    ElDialog,
    ElDivider,
    ElDrawer,
    ElDropdown,
    ElDropdownItem,
    ElDropdownMenu,
    ElEmpty,
    ElResult,
    ElForm,
    ElFormItem,
    ElIcon,
    ElInput,
    ElInputNumber,
    ElLink,
    ElMenu,
    ElMenuItem,
    ElMenuItemGroup,
    ElMessage,
    ElMessageBox,
    ElOption,
    ElOptionGroup,
    ElPagination,
    ElProgress,
    ElRadio,
    ElRadioButton,
    ElRadioGroup,
    ElRow,
    ElScrollbar,
    ElSelect,
    ElStep,
    ElSteps,
    ElSubMenu,
    ElSwitch,
    ElTabPane,
    ElTable,
    ElTableColumn,
    ElTabs,
    ElTag,
    ElTimeline,
    ElTimelineItem,
    ElTooltip,
    ElUpload,
} from 'element-plus'
/* ======Element-Plus样式引入开始======= */
// 基础
import "element-plus/es/components/button/style/css";
import "element-plus/es/components/button-group/style/css";
import "element-plus/es/components/icon/style/css";
import "element-plus/es/components/scrollbar/style/css";
import "element-plus/es/components/tag/style/css";
import "element-plus/es/components/link/style/css";
import "element-plus/es/components/divider/style/css";
import "element-plus/es/components/badge/style/css";
import "element-plus/es/components/avatar/style/css";
import "element-plus/es/components/progress/style/css";
// 布局
import "element-plus/es/components/row/style/css";
import "element-plus/es/components/col/style/css";
// 数据展示
import "element-plus/es/components/table/style/css";
import "element-plus/es/components/table-column/style/css";
import "element-plus/es/components/descriptions/style/css";
import "element-plus/es/components/descriptions-item/style/css";
import "element-plus/es/components/empty/style/css";
import "element-plus/es/components/pagination/style/css";
import "element-plus/es/components/card/style/css";
import "element-plus/es/components/timeline/style/css";
import "element-plus/es/components/timeline-item/style/css";
import "element-plus/es/components/steps/style/css";
import "element-plus/es/components/step/style/css";
import "element-plus/es/components/tabs/style/css";
import "element-plus/es/components/tab-pane/style/css";
// 消息相关
import "element-plus/es/components/message/style/css";
import "element-plus/es/components/message-box/style/css";
import "element-plus/es/components/dialog/style/css";
import "element-plus/es/components/drawer/style/css";
import "element-plus/es/components/alert/style/css";
// 表单
import "element-plus/es/components/form/style/css";
import "element-plus/es/components/form-item/style/css";
import "element-plus/es/components/input/style/css";
import "element-plus/es/components/input-number/style/css";
import "element-plus/es/components/upload/style/css";
import "element-plus/es/components/select/style/css";
import "element-plus/es/components/option/style/css";
import "element-plus/es/components/option-group/style/css";
import "element-plus/es/components/date-picker/style/css";
import "element-plus/es/components/radio/style/css";
import "element-plus/es/components/radio-button/style/css";
import "element-plus/es/components/radio-group/style/css";
import "element-plus/es/components/checkbox/style/css";
import "element-plus/es/components/checkbox-group/style/css";
import "element-plus/es/components/switch/style/css";
import "element-plus/es/components/color-picker/style/css";
// 导航
import "element-plus/es/components/menu/style/css";
import "element-plus/es/components/sub-menu/style/css";
import "element-plus/es/components/menu-item/style/css";
import "element-plus/es/components/menu-item-group/style/css";
import "element-plus/es/components/dropdown/style/css";
import "element-plus/es/components/dropdown-item/style/css";
import "element-plus/es/components/dropdown-menu/style/css";
// 提示
import "element-plus/es/components/tooltip/style/css";
/* ======Element-Plus样式引入结束======= */

export const pushElement = (app: Vpp) => {
    // 基础
    app.component("el-button", ElButton as Component)
        .component("el-button-group", ElButtonGroup as Component)
        .component("el-icon", ElIcon as Component)
        .component("el-scrollbar", ElScrollbar as Component)
        .component("el-tag", ElTag as Component)
        .component("el-link", ElLink as Component)
        .component("el-divider", ElDivider as Component)
        .component("el-badge", ElBadge as Component)
        .component("el-avatar", ElAvatar as Component)
        .component("el-progress", ElProgress as Component)
        // 布局
        .component("el-row", ElRow as Component)
        .component("el-col", ElCol as Component)
        // 数据展示
        .component("el-table", ElTable as Component)
        .component("el-table-column", ElTableColumn as Component)
        .component("el-descriptions", ElDescriptions as Component)
        .component("el-descriptions-item", ElDescriptionsItem as Component)
        .component("el-empty", ElEmpty as Component)
        .component("el-result", ElResult as Component)
        .component("el-pagination", ElPagination as Component)
        .component("el-card", ElCard as Component)
        .component("el-timeline", ElTimeline as Component)
        .component("el-timeline-item", ElTimelineItem as Component)
        .component("el-steps", ElSteps as Component)
        .component("el-step", ElStep as Component)
        .component("el-tabs", ElTabs as Component)
        .component("el-tab-pane", ElTabPane as Component)
        // 消息相关
        .component(ElMessage.name, ElMessage as Component)
        .component(ElMessageBox.name, ElMessageBox as Component)
        .component("el-dialog", ElDialog as Component)
        .component("el-drawer", ElDrawer as Component)
        .component("el-alert", ElAlert as Component)
        // 表单
        .component("el-form", ElForm as Component)
        .component("el-form-item", ElFormItem as Component)
        .component("el-input", ElInput as Component)
        .component("el-input-number", ElInputNumber as Component)
        .component("el-upload", ElUpload as Component)
        .component("el-select", ElSelect as Component)
        .component("el-option", ElOption as Component)
        .component("el-option-group", ElOptionGroup as Component)
        .component("el-date-picker", ElDatePicker as Component)
        .component("el-radio-group", ElRadioGroup as Component)
        .component("el-radio", ElRadio as Component)
        .component("el-radio-button", ElRadioButton as Component)
        .component("el-checkbox-group", ElCheckboxGroup as Component)
        .component("el-checkbox", ElCheckbox as Component)
        .component("el-switch", ElSwitch as Component)
        .component("el-color-picker", ElColorPicker as Component)
        // 菜单
        .component("el-menu", ElMenu as Component)
        .component("el-sub-menu", ElSubMenu as Component)
        .component("el-menu-item", ElMenuItem as Component)
        .component("el-menu-item-group", ElMenuItemGroup as Component)
        .component("el-dropdown", ElDropdown as Component)
        .component("el-dropdown-item", ElDropdownItem as Component)
        .component("el-dropdown-menu", ElDropdownMenu as Component)
        // 提示
        .component("el-tooltip", ElTooltip as Component)
}
