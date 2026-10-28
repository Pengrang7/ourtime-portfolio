import React, { useState } from 'react';
import styled from 'styled-components';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { memoryApi, commentApi, userApi } from '../services/api';
import { CreateCommentRequest } from '../types';
import toast from 'react-hot-toast';

const MemoryDetailContainer = styled.div`
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
`;

const BackButton = styled.button`
  background: none;
  border: none;
  color: #1976d2;
  cursor: pointer;
  font-size: 16px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  
  &:hover {
    text-decoration: underline;
  }
`;

const MemoryCard = styled.div`
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
`;

const MemoryTitle = styled.h1`
  margin: 0 0 16px 0;
  color: #333;
  font-size: 28px;
  font-weight: 700;
`;

const AuthorSection = styled.div`
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
`;

const AuthorAvatar = styled.div`
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8B5CF6 0%, #A855F7 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
`;

const AuthorInfo = styled.div`
  flex: 1;
`;

const AuthorName = styled.div`
  font-weight: 600;
  color: #1f2937;
  font-size: 18px;
  margin-bottom: 2px;
`;

const AuthorTag = styled.div`
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
`;

const MemoryDescription = styled.p`
  color: #333;
  line-height: 1.6;
  margin-bottom: 20px;
`;

const MemoryMeta = styled.div`
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
`;

const ImageGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
`;

const Image = styled.img`
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
`;

const Tags = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
`;

const Tag = styled.span`
  background: #e3f2fd;
  color: #1976d2;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
`;

const ActionButtons = styled.div`
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
`;

const ActionButton = styled.button<{ liked?: boolean }>`
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  
  ${props => props.liked ? `
    background: #d32f2f;
    color: white;
  ` : `
    background: #f5f5f5;
    color: #333;
    
    &:hover {
      background: #e0e0e0;
    }
  `}
`;

const CommentsSection = styled.div`
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
`;

const CommentsTitle = styled.h2`
  margin: 0 0 20px 0;
  color: #333;
  font-size: 20px;
  font-weight: 600;
`;

const CommentForm = styled.form`
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
`;

const CommentInput = styled.input`
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  
  &:focus {
    outline: none;
    border-color: #1976d2;
  }
`;

const CommentButton = styled.button`
  padding: 12px 24px;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  
  &:hover {
    background: #1565c0;
  }
  
  &:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
`;

const CommentList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

const CommentItem = styled.div`
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
`;

const CommentHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

const CommentAuthor = styled.span`
  font-weight: 600;
  color: #333;
`;

const CommentDate = styled.span`
  font-size: 12px;
  color: #666;
`;

const CommentContent = styled.p`
  margin: 0;
  color: #333;
  line-height: 1.5;
`;

const MemoryDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [newComment, setNewComment] = useState('');
  const queryClient = useQueryClient();

  // 현재 로그인한 사용자 정보
  const { data: currentUser } = useQuery('userProfile', userApi.getProfile);

  const { data: memory, isLoading } = useQuery(
    ['memory', id],
    () => memoryApi.getMemory(Number(id)),
    {
      enabled: !!id,
      onError: (error: any) => {
        toast.error('메모리를 불러오는데 실패했습니다.');
        console.error('Memory fetch error:', error);
      }
    }
  );

  // 댓글 조회
  const { data: comments = [] } = useQuery(
    ['comments', id],
    () => commentApi.getComments(Number(id)),
    {
      enabled: !!id,
      onError: (error: any) => {
        console.error('Comments fetch error:', error);
      }
    }
  );

  const likeMutation = useMutation(
    () => memoryApi.likeMemory(Number(id)),
    {
      onSuccess: () => {
        queryClient.invalidateQueries(['memory', id]);
        toast.success('좋아요를 눌렀습니다!');
      },
      onError: (error: any) => {
        toast.error('좋아요 처리에 실패했습니다.');
        console.error('Like error:', error);
      }
    }
  );

  const unlikeMutation = useMutation(
    () => memoryApi.unlikeMemory(Number(id)),
    {
      onSuccess: () => {
        queryClient.invalidateQueries(['memory', id]);
        toast.success('좋아요를 취소했습니다.');
      },
      onError: (error: any) => {
        toast.error('좋아요 취소에 실패했습니다.');
        console.error('Unlike error:', error);
      }
    }
  );

  const createCommentMutation = useMutation(
    (data: CreateCommentRequest) => commentApi.createComment(data),
    {
      onSuccess: () => {
        queryClient.invalidateQueries(['comments', id]);
        queryClient.invalidateQueries(['memory', id]);
        setNewComment('');
        toast.success('댓글이 작성되었습니다!');
      },
      onError: (error: any) => {
        toast.error('댓글 작성에 실패했습니다.');
        console.error('Create comment error:', error);
      }
    }
  );

  const deleteMemoryMutation = useMutation(
    () => memoryApi.deleteMemory(Number(id)),
    {
      onSuccess: () => {
        toast.success('메모리가 삭제되었습니다.');
        navigate('/');
      },
      onError: (error: any) => {
        toast.error('메모리 삭제에 실패했습니다.');
        console.error('Delete memory error:', error);
      }
    }
  );

  const handleLike = () => {
    if (isLiked) {
      unlikeMutation.mutate();
    } else {
      likeMutation.mutate();
    }
  };

  const handleCommentSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim() || !id) return;
    
    createCommentMutation.mutate({
      memoryId: Number(id),
      content: newComment.trim()
    });
  };

  const handleDelete = () => {
    if (window.confirm('정말로 이 메모리를 삭제하시겠습니까?')) {
      deleteMemoryMutation.mutate();
    }
  };

  const handleEdit = () => {
    // 메모리 수정은 지도 페이지에서 해야 하므로 이동
    toast('메모리 수정은 지도에서 메모리를 클릭하여 수정할 수 있습니다.', {
      icon: 'ℹ️',
    });
    navigate('/');
  };

  if (isLoading) {
    return (
      <MemoryDetailContainer>
        <div style={{ textAlign: 'center', padding: '40px' }}>
          로딩 중...
        </div>
      </MemoryDetailContainer>
    );
  }

  if (!memory) {
    return (
      <MemoryDetailContainer>
        <div style={{ textAlign: 'center', padding: '40px' }}>
          <h2>메모리를 찾을 수 없습니다</h2>
        </div>
      </MemoryDetailContainer>
    );
  }

  // 작성자 확인
  const isAuthor = currentUser && memory.user.id === currentUser.id;
  // Note: 백엔드에서 현재 사용자의 좋아요 여부를 전달하면 더 좋습니다
  const isLiked = false; // 임시로 false 설정

  return (
    <MemoryDetailContainer>
      <BackButton onClick={() => window.history.back()}>
        ← 뒤로가기
      </BackButton>

      <MemoryCard>
        <MemoryTitle>{memory.title}</MemoryTitle>
        
        <AuthorSection>
          <AuthorAvatar>
            {memory.user.profileImage ? (
              <img 
                src={memory.user.profileImage} 
                alt={memory.user.nickname}
                style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
              />
            ) : (
              memory.user.nickname.charAt(0).toUpperCase()
            )}
          </AuthorAvatar>
          <AuthorInfo>
            <AuthorName>{memory.user.nickname}</AuthorName>
            {memory.user.userTag && (
              <AuthorTag>@{memory.user.userTag}</AuthorTag>
            )}
          </AuthorInfo>
        </AuthorSection>

        <MemoryMeta>
          <span>📅 {new Date(memory.visitedAt).toLocaleDateString()}</span>
          <span>❤️ {memory.likeCount}개</span>
        </MemoryMeta>

        {memory.description && (
          <MemoryDescription>{memory.description}</MemoryDescription>
        )}

        {memory.imageUrls && memory.imageUrls.length > 0 && (
          <ImageGrid>
            {memory.imageUrls.map((image, index) => (
              <Image key={index} src={image} alt={`Memory ${index + 1}`} />
            ))}
          </ImageGrid>
        )}

        {memory.tags.length > 0 && (
          <Tags>
            {memory.tags.map(tag => (
              <Tag key={tag.id}>#{tag.name}</Tag>
            ))}
          </Tags>
        )}

        <ActionButtons>
          <ActionButton 
            liked={isLiked}
            onClick={handleLike}
            disabled={likeMutation.isLoading || unlikeMutation.isLoading}
          >
            {isLiked ? '❤️ 좋아요 취소' : '🤍 좋아요'}
          </ActionButton>
          
          {isAuthor && (
            <>
              <ActionButton onClick={handleEdit}>
                ✏️ 수정
              </ActionButton>
              <ActionButton 
                onClick={handleDelete}
                disabled={deleteMemoryMutation.isLoading}
                style={{ background: '#ef4444', color: 'white' }}
              >
                🗑️ 삭제
              </ActionButton>
            </>
          )}
        </ActionButtons>
      </MemoryCard>

      <CommentsSection>
        <CommentsTitle>댓글 ({comments.length})</CommentsTitle>
        
        <CommentForm onSubmit={handleCommentSubmit}>
          <CommentInput
            type="text"
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="댓글을 작성하세요..."
            disabled={createCommentMutation.isLoading}
          />
          <CommentButton 
            type="submit"
            disabled={!newComment.trim() || createCommentMutation.isLoading}
          >
            {createCommentMutation.isLoading ? '작성 중...' : '작성'}
          </CommentButton>
        </CommentForm>

        <CommentList>
          {comments.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '20px', color: '#666' }}>
              첫 댓글을 작성해보세요!
            </div>
          ) : (
            comments.map(comment => (
              <CommentItem key={comment.id}>
                <CommentHeader>
                  <CommentAuthor>{comment.user.nickname}</CommentAuthor>
                  {comment.user.userTag && (
                    <span style={{ color: '#666', fontSize: '12px' }}>@{comment.user.userTag}</span>
                  )}
                  <CommentDate>
                    {new Date(comment.createdAt).toLocaleString()}
                  </CommentDate>
                </CommentHeader>
                <CommentContent>{comment.content}</CommentContent>
              </CommentItem>
            ))
          )}
        </CommentList>
      </CommentsSection>
    </MemoryDetailContainer>
  );
};

export default MemoryDetail;