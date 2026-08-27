package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.service.IImFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文件信息表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-08-27
 */
@Service
public class ImFileServiceImpl extends ServiceImpl<ImFileMapper, ImFile> implements IImFileService {

}
