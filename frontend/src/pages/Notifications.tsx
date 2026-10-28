import React from 'react';
import styled from 'styled-components';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { invitationApi } from '../services/api';
import toast from 'react-hot-toast';

interface Invitation {
  id: number;
  groupId: number;
  groupName: string;
  inviterId: number;
  inviterNickname: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
  respondedAt?: string;
}

const Notifications: React.FC = () => {
  const queryClient = useQueryClient();

  const { data: invitations = [], isLoading } = useQuery<Invitation[]>(
    'invitations',
    invitationApi.getPendingInvitations,
    {
      onError: (error: any) => {
        toast.error('초대 목록을 불러오는데 실패했습니다.');
        console.error('Invitations fetch error:', error);
      }
    }
  );

  const acceptMutation = useMutation(
    (invitationId: number) => invitationApi.acceptInvitation(invitationId),
    {
      onSuccess: () => {
        toast.success('초대를 수락했습니다!');
        queryClient.invalidateQueries('invitations');
        queryClient.invalidateQueries('groups');
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '초대 수락에 실패했습니다.');
      }
    }
  );

  const rejectMutation = useMutation(
    (invitationId: number) => invitationApi.rejectInvitation(invitationId),
    {
      onSuccess: () => {
        toast.success('초대를 거절했습니다.');
        queryClient.invalidateQueries('invitations');
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '초대 거절에 실패했습니다.');
      }
    }
  );

  const handleAccept = (invitationId: number) => {
    acceptMutation.mutate(invitationId);
  };

  const handleReject = (invitationId: number) => {
    if (window.confirm('이 초대를 거절하시겠습니까?')) {
      rejectMutation.mutate(invitationId);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (isLoading) {
    return (
      <Container>
        <Title>📬 알림</Title>
        <div style={{ textAlign: 'center', padding: '40px' }}>
          로딩 중...
        </div>
      </Container>
    );
  }

  return (
    <Container>
      <Title>📬 알림</Title>

      {invitations.length === 0 ? (
        <EmptyState>
          <EmptyIcon>🔔</EmptyIcon>
          <EmptyText>새로운 알림이 없습니다</EmptyText>
          <EmptySubText>그룹 초대를 받으면 여기에 표시됩니다</EmptySubText>
        </EmptyState>
      ) : (
        <InvitationList>
          {invitations.map((invitation) => (
            <InvitationCard key={invitation.id}>
              <InvitationHeader>
                <GroupIcon>👥</GroupIcon>
                <InvitationInfo>
                  <GroupName>{invitation.groupName}</GroupName>
                  <InviterText>
                    <strong>{invitation.inviterNickname}</strong>님이 초대했습니다
                  </InviterText>
                  <TimeText>{formatDate(invitation.createdAt)}</TimeText>
                </InvitationInfo>
              </InvitationHeader>

              <ButtonGroup>
                <AcceptButton
                  onClick={() => handleAccept(invitation.id)}
                  disabled={acceptMutation.isLoading || rejectMutation.isLoading}
                >
                  {acceptMutation.isLoading ? '수락 중...' : '✓ 수락'}
                </AcceptButton>
                <RejectButton
                  onClick={() => handleReject(invitation.id)}
                  disabled={acceptMutation.isLoading || rejectMutation.isLoading}
                >
                  {rejectMutation.isLoading ? '거절 중...' : '✕ 거절'}
                </RejectButton>
              </ButtonGroup>
            </InvitationCard>
          ))}
        </InvitationList>
      )}
    </Container>
  );
};

// Styled Components
const Container = styled.div`
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
`;

const Title = styled.h1`
  margin-bottom: 32px;
  color: #111827;
  font-size: 32px;
  font-weight: 700;
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
`;

const EmptyIcon = styled.div`
  font-size: 64px;
  margin-bottom: 16px;
`;

const EmptyText = styled.h3`
  margin: 0 0 8px 0;
  color: #374151;
  font-size: 20px;
`;

const EmptySubText = styled.p`
  margin: 0;
  color: #9ca3af;
  font-size: 14px;
`;

const InvitationList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

const InvitationCard = styled.div`
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

const InvitationHeader = styled.div`
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
`;

const GroupIcon = styled.div`
  font-size: 32px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border-radius: 12px;
`;

const InvitationInfo = styled.div`
  flex: 1;
`;

const GroupName = styled.h3`
  margin: 0 0 8px 0;
  color: #111827;
  font-size: 18px;
  font-weight: 600;
`;

const InviterText = styled.p`
  margin: 0 0 4px 0;
  color: #374151;
  font-size: 14px;

  strong {
    color: #3b82f6;
    font-weight: 600;
  }
`;

const TimeText = styled.p`
  margin: 0;
  color: #9ca3af;
  font-size: 12px;
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 12px;
`;

const Button = styled.button`
  flex: 1;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const AcceptButton = styled(Button)`
  background: #10b981;
  color: white;
  border: none;

  &:hover:not(:disabled) {
    background: #059669;
  }
`;

const RejectButton = styled(Button)`
  background: white;
  color: #ef4444;
  border: 1px solid #ef4444;

  &:hover:not(:disabled) {
    background: #fef2f2;
  }
`;

export default Notifications;

