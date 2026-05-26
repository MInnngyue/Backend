package com.lostfound.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.dto.PostPublishDTO;
import com.lostfound.backend.dto.PostQueryDTO;
import com.lostfound.backend.entity.Post;
import com.lostfound.backend.entity.PostImage;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.PostImageMapper;
import com.lostfound.backend.mapper.PostMapper;
import com.lostfound.backend.mapper.UserMapper;
import com.lostfound.backend.service.MatchingService;
import com.lostfound.backend.service.PostService;
import com.lostfound.backend.vo.PostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final UserMapper userMapper;
    private final MatchingService matchingService;

    @Override
    public Page<PostVO> page(PostQueryDTO query) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .notIn(Post::getStatus, 4, 5)
                .eq(query.getType() != null, Post::getType, query.getType())
                .eq(query.getItemCategory() != null && !query.getItemCategory().isEmpty(),
                        Post::getItemCategory, query.getItemCategory())
                .eq(query.getStatus() != null, Post::getStatus, query.getStatus())
                .and(query.getKeyword() != null && !query.getKeyword().isEmpty(),
                        w -> w.like(Post::getTitle, query.getKeyword())
                             .or()
                             .like(Post::getDescription, query.getKeyword()))
                .orderByDesc(Post::getCreateTime);

        long total = postMapper.selectCount(wrapper);
        int offset = (query.getPage() - 1) * query.getPageSize();
        // 安全：offset 和 pageSize 均为 int 类型，String.format %d 强制整数格式化
        List<Post> records = postMapper.selectList(
                wrapper.last(String.format("LIMIT %d,%d", offset, query.getPageSize())));

        Page<PostVO> result = new Page<>(query.getPage(), query.getPageSize(), total);
        result.setRecords(records.stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public PostVO detail(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        // 增加浏览次数
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);

        PostVO vo = toVO(post);
        // 加载完整图片列表
        vo.setCoverImage(null); // detail 页不用 cover，用完整列表
        return vo;
    }

    @Override
    @Transactional
    public PostVO publish(Long userId, PostPublishDTO dto) {
        Post post = new Post();
        post.setUserId(userId);
        post.setType(dto.getType());
        post.setItemCategory(dto.getItemCategory());
        post.setColor(dto.getColor());
        post.setLocationCampus(dto.getLocationCampus());
        post.setLocationArea(dto.getLocationArea());
        post.setLocationDetail(dto.getLocationDetail());
        post.setLostTime(dto.getLostTime());
        post.setTitle(dto.getTitle());
        post.setDescription(dto.getDescription());
        post.setStatus(0);
        post.setViewCount(0);
        post.setReviewStatus(0);
        postMapper.insert(post);

        // 保存图片
        if (dto.getImages() != null) {
            for (int i = 0; i < dto.getImages().length; i++) {
                PostImage img = new PostImage();
                img.setPostId(post.getId());
                img.setImageUrl(dto.getImages()[i]);
                img.setSortOrder(i);
                postImageMapper.insert(img);
            }
        }

        // 异步触发匹配（异常不影响发布）
        try {
            matchingService.match(post);
        } catch (Exception e) {
            log.warn("匹配计算异常: {}", e.getMessage());
        }

        return toVO(post);
    }

    @Override
    public void complete(Long id, Long userId) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        if (!post.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");
        if (post.getStatus() != 0 && post.getStatus() != 1) throw new BusinessException(400, "当前状态不可完结");
        post.setStatus(3);
        postMapper.updateById(post);
    }

    @Override
    public void remove(Long id, Long userId) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        postMapper.deleteById(id);
    }

    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setType(post.getType());
        vo.setItemCategory(post.getItemCategory());
        vo.setColor(post.getColor());
        vo.setLocationCampus(post.getLocationCampus());
        vo.setLocationArea(post.getLocationArea());
        vo.setLocationDetail(post.getLocationDetail());
        vo.setLostTime(post.getLostTime());
        vo.setTitle(post.getTitle());
        vo.setDescription(post.getDescription());
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount());
        vo.setReviewStatus(post.getReviewStatus());
        vo.setUserId(post.getUserId());
        vo.setCreateTime(post.getCreateTime());

        // 发布者信息
        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setCreditScore(user.getCreditScore());
        }

        // 所有图片
        List<PostImage> images = postImageMapper.selectList(new LambdaQueryWrapper<PostImage>()
                .eq(PostImage::getPostId, post.getId())
                .orderByAsc(PostImage::getSortOrder));
        if (!images.isEmpty()) {
            vo.setCoverImage(images.get(0).getImageUrl());
            vo.setImages(images.stream().map(PostImage::getImageUrl).toList());
        }

        return vo;
    }
}
