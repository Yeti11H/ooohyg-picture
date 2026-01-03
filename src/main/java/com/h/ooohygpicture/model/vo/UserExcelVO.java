package com.h.ooohygpicture.model.vo;



import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class UserExcelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * @ExcelProperty: 指定 Excel 表头名称
     * @ColumnWidth: 指定列宽
     */
    @ExcelProperty("用户ID")
    @ColumnWidth(20)
    private Long id;

    @ExcelProperty("用户账号")
    @ColumnWidth(20)
    private String userAccount;

    @ExcelProperty("用户昵称")
    @ColumnWidth(20)
    private String userName;

    @ExcelProperty("用户角色")
    @ColumnWidth(15)
    private String userRole;

    @ExcelProperty("注册时间")
    @ColumnWidth(25)
    // 这里注意：EasyExcel 对 LocalDateTime 支持有时需要转换，或者直接用 String
    // 为了省事，你可以转成 Date 或者 String，这里假设你转成了 String 或者配置了转换器
    private String createTime;
}