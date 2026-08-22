package msr.atsulab.app.data.datasource

import com.apollographql.apollo3.api.ApolloResponse
import msr.atsulab.app.*
import msr.atsulab.app.type.ActivityType
import msr.atsulab.app.type.LikeableType
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface SocialDataSource {
    fun getSocialData(): Observable<ApolloResponse<SocialDataQuery.Data>>
    fun getActivityDetail(id: Int): Observable<ApolloResponse<ActivityQuery.Data>>
    fun getActivityList(page: Int, userId: Int?, typeIn: List<ActivityType>?, isFollowing: Boolean?): Observable<ApolloResponse<ActivityListQuery.Data>>
    fun toggleActivitySubscription(id: Int, isSubscribe: Boolean): Completable
    fun toggleLike(id: Int, likeableType: LikeableType): Completable
    fun deleteActivity(id: Int): Completable
    fun deleteActivityReply(id: Int): Completable
    fun saveTextActivity(id: Int?, text: String): Observable<ApolloResponse<SaveTextActivityMutation.Data>>
    fun saveActivityReply(id: Int?, activityId: Int, text: String): Observable<ApolloResponse<SaveActivityReplyMutation.Data>>
    fun saveMessageActivity(id: Int?, recipientId: Int, message: String, private: Boolean): Observable<ApolloResponse<SaveMessageActivityMutation.Data>>
}