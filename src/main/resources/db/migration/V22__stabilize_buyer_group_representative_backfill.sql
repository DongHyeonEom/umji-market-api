UPDATE buyer_group
JOIN (
    SELECT buyer_group_id, MIN(account_id) AS account_id
    FROM buyer_group_member
    WHERE status = 'ACTIVE'
    GROUP BY buyer_group_id
) initial_representative ON initial_representative.buyer_group_id = buyer_group.id
SET buyer_group.representative_account_id = initial_representative.account_id;
