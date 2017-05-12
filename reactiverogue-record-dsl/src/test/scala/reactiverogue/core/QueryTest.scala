package reactiverogue.core

import java.util.regex.Pattern

import org.joda.time.{DateTime, DateTimeZone}
import org.junit._
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite
import reactivemongo.bson._
import reactiverogue.bson.BSONSerializable
import reactiverogue.core.QueryImplicits._
import reactiverogue.record._

import scala.language.postfixOps

class QueryTest extends JUnitSuite with Matchers {

  def dateToByteArray(d: DateTime): Array[Byte] = {
    val b = new Array[Byte](12)
    val bb = java.nio.ByteBuffer.wrap(b)
    bb.putInt((d.getMillis() / 1000).intValue())
    bb.putInt(0)
    bb.putInt(0)
    b
  }

  def simpleObjectId(d: DateTime): BSONObjectID =
    BSONObjectID(dateToByteArray(d))

  @Test
  def testProduceACorrectJSONQueryString {
    val d1 = new DateTime(2010, 5, 1, 0, 0, 0, 0, DateTimeZone.UTC)
    val d2 = new DateTime(2010, 5, 2, 0, 0, 0, 0, DateTimeZone.UTC)
    val oid1 = simpleObjectId(d1)
    val oid2 = simpleObjectId(d2)
    val oid = BSONObjectID.generate
    val ven1 = Venue.createRecord.id(oid1)

    // eqs
    Venue.where(_.mayor eqs 1).toString() shouldBe """db.venues.find({"mayor":1})"""
    Venue
      .where(_.venuename eqs "Starbucks")
      .toString() shouldBe """db.venues.find({"venuename":"Starbucks"})"""
    Venue.where(_.closed eqs true).toString() shouldBe """db.venues.find({"closed":true})"""
    //    Venue.where(_.id eqs oid).toString() shouldBe ("""db.venues.find({"_id":"%s"})""" format oid.stringify)
    VenueClaim
      .where(_.status eqs ClaimStatus.approved)
      .toString() shouldBe """db.venueclaims.find({"status":"Approved"})"""

    //    VenueClaim.where(_.venueid eqs oid).toString() shouldBe ("""db.venueclaims.find({"vid":"%s"})""" format oid.stringify)
    //    VenueClaim.where(_.venueid eqs ven1.id.value).toString() shouldBe ("""db.venueclaims.find({"vid":"%s"})""" format oid1.stringify)
    //    VenueClaim.where(_.venueid eqs ven1)    .toString() shouldBe ("""db.venueclaims.find({"vid":"%s"})""" format oid1.stringify)

    Venue
      .where(_.zipCode eqs None)
      .toString shouldBe """db.venues.find({"zipCode":{"$exists":false}})"""
    Venue.where(_.zipCode eqs "60606").toString shouldBe """db.venues.find({"zipCode":"60606"})"""
    Venue
      .where(_.zipCode eqs Some("60606"))
      .toString shouldBe """db.venues.find({"zipCode":"60606"})"""

    // neq,lt,gt
    Venue
      .where(_.mayor_count neqs 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$ne":5}})"""
    Venue
      .where(_.mayor_count < 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$lt":5}})"""
    Venue
      .where(_.mayor_count lt 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$lt":5}})"""
    Venue
      .where(_.mayor_count <= 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$lte":5}})"""
    Venue
      .where(_.mayor_count lte 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$lte":5}})"""
    Venue
      .where(_.mayor_count > 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$gt":5}})"""
    Venue
      .where(_.mayor_count gt 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$gt":5}})"""
    Venue
      .where(_.mayor_count >= 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$gte":5}})"""
    Venue
      .where(_.mayor_count gte 5)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$gte":5}})"""
    Venue
      .where(_.mayor_count between (3, 5))
      .toString() shouldBe """db.venues.find({"mayor_count":{"$gte":3,"$lte":5}})"""
    Venue
      .where(_.popularity < 4)
      .toString() shouldBe """db.venues.find({"popularity":{"$lt":4}})"""
    VenueClaim
      .where(_.status neqs ClaimStatus.approved)
      .toString() shouldBe """db.venueclaims.find({"status":{"$ne":"Approved"}})"""
    VenueClaim
      .where(_.reason eqs RejectReason.tooManyClaims)
      .toString() shouldBe """db.venueclaims.find({"reason":0})"""
    VenueClaim
      .where(_.reason eqs RejectReason.cheater)
      .toString() shouldBe """db.venueclaims.find({"reason":1})"""
    VenueClaim
      .where(_.reason eqs RejectReason.wrongCode)
      .toString() shouldBe """db.venueclaims.find({"reason":2})"""

    // comparison even when type information is unavailable
    def doLessThan[M <: MongoRecord[M], T: BSONSerializable](meta: M with MongoMetaRecord[M],
                                                             f: M => Field[T, M],
                                                             otherVal: T) =
      meta.where(r => f(r) < otherVal)
    doLessThan(Venue, (v: Venue) => v.mayor_count, 5L)
      .toString() shouldBe """db.venues.find({"mayor_count":{"$lt":5}})"""

    // in,nin
    Venue
      .where(_.legacyid in List(123L, 456L))
      .toString() shouldBe """db.venues.find({"legid":{"$in":[123,456]}})"""
    Venue
      .where(_.venuename nin List("Starbucks", "Whole Foods"))
      .toString() shouldBe """db.venues.find({"venuename":{"$nin":["Starbucks","Whole Foods"]}})"""
    VenueClaim
      .where(_.status in List(ClaimStatus.approved, ClaimStatus.pending))
      .toString() shouldBe """db.venueclaims.find({"status":{"$in":["Approved","Pending approval"]}})"""
    VenueClaim
      .where(_.status nin List(ClaimStatus.approved, ClaimStatus.pending))
      .toString() shouldBe """db.venueclaims.find({"status":{"$nin":["Approved","Pending approval"]}})"""

    //    VenueClaim.where(_.venueid in List(ven1.id.value)).toString() shouldBe ("""db.venueclaims.find({"vid":{"$in":["%s"]}})""" format oid1.stringify)
    //    VenueClaim.where(_.venueid in List(ven1))    .toString() shouldBe ("""db.venueclaims.find({"vid":{"$in":["%s"]}})""" format oid1.stringify)

    //    VenueClaim.where(_.venueid nin List(ven1.id.value)).toString() shouldBe ("""db.venueclaims.find({"vid":{"$nin":["%s"]}})""" format oid1.stringify)
    //    VenueClaim.where(_.venueid nin List(ven1))     .toString() shouldBe ("""db.venueclaims.find({"vid":{"$nin":["%s"]}})""" format oid1.stringify)

    // exists
    Venue
      .where(_.id exists true)
      .toString() shouldBe """db.venues.find({"_id":{"$exists":true}})"""

    // startsWith, regex
    Venue
      .where(_.venuename startsWith "Starbucks")
      .toString() shouldBe """db.venues.find({"venuename":{"$regex":"^\\QStarbucks\\E"}})"""
    val p1 = Pattern.compile("Star.*")
    Venue
      .where(_.venuename regexWarningNotIndexed p1)
      .toString() shouldBe """db.venues.find({"venuename":{"$regex":"Star.*"}})"""
    Venue
      .where(_.venuename matches p1)
      .toString() shouldBe """db.venues.find({"venuename":{"$regex":"Star.*"}})"""
    val p2 = Pattern.compile("Star.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    Venue
      .where(_.venuename matches p2)
      .toString() shouldBe """db.venues.find({"venuename":{"$regex":"Star.*","$options":"im"}})"""

    //text search
    Venue
      .textSearch("Starbucks")
      .toString() shouldBe """db.venues.find({"$text":{"$search":"Starbucks"}})"""

    // all, in, size, contains, at
    Venue
      .where(_.tags all List("db", "ka"))
      .toString() shouldBe """db.venues.find({"tags":{"$all":["db","ka"]}})"""
    Venue
      .where(_.tags in List("db", "ka"))
      .toString() shouldBe """db.venues.find({"tags":{"$in":["db","ka"]}})"""
    Venue
      .where(_.tags nin List("db", "ka"))
      .toString() shouldBe """db.venues.find({"tags":{"$nin":["db","ka"]}})"""
    Venue
      .where(_.tags neqs List("db", "ka"))
      .toString() shouldBe """db.venues.find({"tags":{"$ne":["db","ka"]}})"""
    Venue
      .where(_.tags matches "kara.*".r)
      .toString() shouldBe """db.venues.find({"tags":{"$regex":"kara.*"}})"""
    Venue.where(_.tags size 3).toString() shouldBe """db.venues.find({"tags":{"$size":3}})"""
    Venue
      .where(_.tags contains "karaoke")
      .toString() shouldBe """db.venues.find({"tags":"karaoke"})"""
    Venue
      .where(_.tags notcontains "karaoke")
      .toString() shouldBe """db.venues.find({"tags":{"$ne":"karaoke"}})"""
    Venue.where(_.popularity contains 3).toString() shouldBe """db.venues.find({"popularity":3})"""
    Venue
      .where(_.popularity at 0 eqs 3)
      .toString() shouldBe """db.venues.find({"popularity.0":3})"""
    //    Venue.where(_.categories at 0 eqs oid).toString() shouldBe """db.venues.find({"categories.0":"%s"})""".format(oid.stringify)
    Venue
      .where(_.tags at 0 startsWith "kara")
      .toString() shouldBe """db.venues.find({"tags.0":{"$regex":"^\\Qkara\\E"}})"""
    // alternative syntax
    Venue
      .where(_.tags idx 0 startsWith "kara")
      .toString() shouldBe """db.venues.find({"tags.0":{"$regex":"^\\Qkara\\E"}})"""
    Venue
      .where(_.tags startsWith "kara")
      .toString() shouldBe """db.venues.find({"tags":{"$regex":"^\\Qkara\\E"}})"""
    Venue
      .where(_.tags matches "k.*".r)
      .toString() shouldBe """db.venues.find({"tags":{"$regex":"k.*"}})"""

    // maps
    Tip.where(_.counts at "foo" eqs 3).toString() shouldBe """db.tips.find({"counts.foo":3})"""

    // near
    Venue
      .where(_.geolatlng near (39.0, -74.0, Degrees(0.2)))
      .toString() shouldBe """db.venues.find({"latlng":{"$near":[39,-74,0.2]}})"""
    Venue
      .where(_.geolatlng withinCircle (1.0, 2.0, Degrees(0.3)))
      .toString() shouldBe """db.venues.find({"latlng":{"$within":{"$center":[[1,2],0.3]}}})"""
    Venue
      .where(_.geolatlng withinBox (1.0, 2.0, 3.0, 4.0))
      .toString() shouldBe """db.venues.find({"latlng":{"$within":{"$box":[[1,2],[3,4]]}}})"""
    Venue
      .where(_.geolatlng eqs (45.0, 50.0))
      .toString() shouldBe """db.venues.find({"latlng":[45,50]})"""
    Venue
      .where(_.geolatlng neqs (31.0, 23.0))
      .toString() shouldBe """db.venues.find({"latlng":{"$ne":[31,23]}})"""
    Venue
      .where(_.geolatlng eqs LatLong(45.0, 50.0))
      .toString() shouldBe """db.venues.find({"latlng":[45,50]})"""
    Venue
      .where(_.geolatlng neqs LatLong(31.0, 23.0))
      .toString() shouldBe """db.venues.find({"latlng":{"$ne":[31,23]}})"""
    Venue
      .where(_.geolatlng nearSphere (39.0, -74.0, Radians(1.0)))
      .toString() shouldBe """db.venues.find({"latlng":{"$nearSphere":[39,-74],"$maxDistance":1}})"""

    // ObjectId before, after, between
    //    Venue.where(_.id before d2).toString() shouldBe """db.venues.find({"_id":{"$lt":"%s"}})""".format(oid2.stringify)
    //    Venue.where(_.id after d1).toString() shouldBe """db.venues.find({"_id":{"$gt":"%s"}})""".format(oid1.stringify)
    //    Venue.where(_.id between (d1, d2)).toString() shouldBe """db.venues.find({"_id":{"$gt":"%s","$lt":"%s"}})""".format(oid1.stringify, oid2.stringify)
    //    Venue.where(_.id between Tuple2(d1, d2)).toString() shouldBe """db.venues.find({"_id":{"$gt":"%s","$lt":"%s"}})""".format(oid1.stringify, oid2.stringify)

    // DateTime before, after, between
    Venue
      .where(_.last_updated before d2)
      .toString() shouldBe """db.venues.find({"last_updated":{"$lt":{"$date":1272758400000}}})"""
    Venue
      .where(_.last_updated after d1)
      .toString() shouldBe """db.venues.find({"last_updated":{"$gt":{"$date":1272672000000}}})"""
    Venue
      .where(_.last_updated between (d1.toDate, d2.toDate))
      .toString() shouldBe """db.venues.find({"last_updated":{"$gte":{"$date":1272672000000},"$lte":{"$date":1272758400000}}})"""
    //    Venue.where(_.last_updated between Tuple2(d1, d2)).toString() shouldBe """db.venues.find({"last_updated":{"$gte":{"$date":1272672000000},"$lte":{"$date":1272758400000}}})"""
    //    Venue.where(_.last_updated eqs d1)          .toString() shouldBe """db.venues.find({"last_updated":{"$date":1272672000000}})"""
    Venue
      .where(_.last_updated eqs d1.toDate)
      .toString() shouldBe """db.venues.find({"last_updated":{"$date":1272672000000}})"""
    Venue
      .where(_.last_updated after d1.toDate)
      .toString() shouldBe """db.venues.find({"last_updated":{"$gt":{"$date":1272672000000}}})"""

    // Case class list field
    Comment
      .where(_.comments.unsafeField[Int]("z") contains 123)
      .toString() shouldBe """db.comments.find({"comments.z":123})"""
    Comment
      .where(_.comments.unsafeField[String]("comment") contains "hi")
      .toString() shouldBe """db.comments.find({"comments.comment":"hi"})"""

    // BsonRecordField subfield queries
    Venue
      .where(_.claims.subfield(_.status) contains ClaimStatus.approved)
      .toString() shouldBe """db.venues.find({"claims.status":"Approved"})"""
    Venue
      .where(_.claims.subfield(_.userid) between (1, 10))
      .toString() shouldBe """db.venues.find({"claims.uid":{"$gte":1,"$lte":10}})"""
    Venue
      .where(_.claims.subfield(_.date) between (d1.toDate, d2.toDate))
      .toString() shouldBe """db.venues.find({"claims.date":{"$gte":{"$date":1272672000000},"$lte":{"$date":1272758400000}}})"""
    Venue
      .where(_.lastClaim.subfield(_.userid) eqs 123)
      .toString() shouldBe """db.venues.find({"lastClaim.uid":123})"""
    Venue
      .where(_.claims.subfield(_.source.subfield(_.name)) contains "twitter")
      .toString() shouldBe """db.venues.find({"claims.source.name":"twitter"})"""

    // Enumeration list
    //    OAuthConsumer.where(_.privileges contains ConsumerPrivilege.awardBadges).toString() shouldBe """db.oauthconsumers.find({"privileges":"Award badges"})"""
    //    OAuthConsumer.where(_.privileges at 0 eqs ConsumerPrivilege.awardBadges).toString() shouldBe """db.oauthconsumers.find({"privileges.0":"Award badges"})"""

    // Field type
    Venue
      .where(_.legacyid hastype MongoType.String)
      .toString() shouldBe """db.venues.find({"legid":{"$type":2}})"""

    // Modulus
    Venue
      .where(_.legacyid mod (5, 1))
      .toString() shouldBe """db.venues.find({"legid":{"$mod":[5,1]}})"""

    // compound queries
    Venue
      .where(_.mayor eqs 1)
      .and(_.tags contains "karaoke")
      .toString() shouldBe """db.venues.find({"mayor":1,"tags":"karaoke"})"""
    Venue
      .where(_.mayor eqs 1)
      .and(_.mayor_count eqs 5)
      .toString() shouldBe """db.venues.find({"mayor":1,"mayor_count":5})"""
    Venue
      .where(_.mayor eqs 1)
      .and(_.mayor_count lt 5)
      .toString() shouldBe """db.venues.find({"mayor":1,"mayor_count":{"$lt":5}})"""
    Venue
      .where(_.mayor eqs 1)
      .and(_.mayor_count gt 3)
      .and(_.mayor_count lt 5)
      .toString() shouldBe """db.venues.find({"mayor":1,"mayor_count":{"$lt":5,"$gt":3}})"""

    // queries with no clauses
    metaRecordToQueryBuilder(Venue).toString() shouldBe "db.venues.find({})"
    Venue.orderDesc(_.id).toString() shouldBe """db.venues.find({}).sort({"_id":-1})"""

    // ordered queries
    Venue
      .where(_.mayor eqs 1)
      .orderAsc(_.legacyid)
      .toString() shouldBe """db.venues.find({"mayor":1}).sort({"legid":1})"""
    Venue
      .where(_.mayor eqs 1)
      .orderDesc(_.legacyid)
      .andAsc(_.userid)
      .toString() shouldBe """db.venues.find({"mayor":1}).sort({"legid":-1,"userid":1})"""
    Venue
      .where(_.mayor eqs 1)
      .orderDesc(_.lastClaim.subfield(_.date))
      .toString() shouldBe """db.venues.find({"mayor":1}).sort({"lastClaim.date":-1})"""
    Venue
      .where(_.mayor eqs 1)
      .orderNaturalAsc
      .toString() shouldBe """db.venues.find({"mayor":1}).sort({"$natural":1})"""
    Venue
      .where(_.mayor eqs 1)
      .orderNaturalDesc
      .toString() shouldBe """db.venues.find({"mayor":1}).sort({"$natural":-1})"""

    // select queries
    Venue
      .where(_.mayor eqs 1)
      .select(_.legacyid)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1})"""
    Venue
      .where(_.mayor eqs 1)
      .select(_.legacyid, _.userid)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1})"""
    Venue
      .where(_.mayor eqs 1)
      .select(_.legacyid, _.userid, _.mayor)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1})"""
    Venue
      .where(_.mayor eqs 1)
      .select(_.legacyid, _.userid, _.mayor, _.mayor_count)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1,"mayor_count":1})"""
    Venue
      .where(_.mayor eqs 1)
      .select(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1,"mayor_count":1,"closed":1})"""
    Venue
      .where(_.mayor eqs 1)
      .select(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed, _.tags)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1,"mayor_count":1,"closed":1,"tags":1})"""

    // select case queries
    Venue
      .where(_.mayor eqs 1)
      .selectCase(_.legacyid, V1)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1})"""
    Venue
      .where(_.mayor eqs 1)
      .selectCase(_.legacyid, _.userid, V2)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1})"""
    Venue
      .where(_.mayor eqs 1)
      .selectCase(_.legacyid, _.userid, _.mayor, V3)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1})"""
    Venue
      .where(_.mayor eqs 1)
      .selectCase(_.legacyid, _.userid, _.mayor, _.mayor_count, V4)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1,"mayor_count":1})"""
    Venue
      .where(_.mayor eqs 1)
      .selectCase(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed, V5)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1,"mayor_count":1,"closed":1})"""
    Venue
      .where(_.mayor eqs 1)
      .selectCase(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed, _.tags, V6)
      .toString() shouldBe """db.venues.find({"mayor":1},{"legid":1,"userid":1,"mayor":1,"mayor_count":1,"closed":1,"tags":1})"""

    // select subfields
    Tip
      .where(_.legacyid eqs 1)
      .select(_.counts at "foo")
      .toString() shouldBe """db.tips.find({"legid":1},{"counts.foo":1})"""
    Venue
      .where(_.legacyid eqs 1)
      .select(_.geolatlng.unsafeField[Double]("lat"))
      .toString() shouldBe """db.venues.find({"legid":1},{"latlng.lat":1})"""
    Venue
      .where(_.legacyid eqs 1)
      .select(_.lastClaim.subselect(_.status))
      .toString() shouldBe """db.venues.find({"legid":1},{"lastClaim.status":1})"""
    Venue
      .where(_.legacyid eqs 1)
      .select(_.claims.subselect(_.userid))
      .toString() shouldBe """db.venues.find({"legid":1},{"claims.uid":1})"""

    // select slice
    Venue
      .where(_.legacyid eqs 1)
      .select(_.tags)
      .toString() shouldBe """db.venues.find({"legid":1},{"tags":1})"""
    Venue
      .where(_.legacyid eqs 1)
      .select(_.tags.slice(4))
      .toString() shouldBe """db.venues.find({"legid":1},{"tags":{"$slice":4}})"""
    Venue
      .where(_.legacyid eqs 1)
      .select(_.tags.slice(4, 7))
      .toString() shouldBe """db.venues.find({"legid":1},{"tags":{"$slice":[4,7]}})"""

    // select $
    Venue
      .where(_.legacyid eqs 1)
      .select(_.tags.$)
      .toString() shouldBe """db.venues.find({"legid":1},{"tags.$":1})"""

    Venue
      .where(_.legacyid eqs 1)
      .and(_.claims elemMatch (_.status eqs ClaimStatus.approved,
      _.userid gt 2097))
      .toString() shouldBe
      """db.venues.find({"legid":1,"claims":{"$elemMatch":{"uid":{"$gt":2097},"status":"Approved"}}})"""

    // TODO: case class list fields
    // Comment.select(_.comments.unsafeField[Long]("userid")).toString() shouldBe """db.venues.find({},{"comments.userid":1})"""

    // out of order and doesn't screw up earlier params
    Venue
      .limit(10)
      .where(_.mayor eqs 1)
      .toString() shouldBe """db.venues.find({"mayor":1}).limit(10)"""
    Venue
      .orderDesc(_.id)
      .and(_.mayor eqs 1)
      .toString() shouldBe """db.venues.find({"mayor":1}).sort({"_id":-1})"""
    Venue
      .orderDesc(_.id)
      .skip(3)
      .and(_.mayor eqs 1)
      .toString() shouldBe """db.venues.find({"mayor":1}).sort({"_id":-1}).skip(3)"""

    // Scan should be the same as and/where
    Venue
      .where(_.mayor eqs 1)
      .scan(_.tags contains "karaoke")
      .toString() shouldBe """db.venues.find({"mayor":1,"tags":"karaoke"})"""
    Venue
      .scan(_.mayor eqs 1)
      .and(_.mayor_count eqs 5)
      .toString() shouldBe """db.venues.find({"mayor":1,"mayor_count":5})"""
    Venue
      .scan(_.mayor eqs 1)
      .scan(_.mayor_count lt 5)
      .toString() shouldBe """db.venues.find({"mayor":1,"mayor_count":{"$lt":5}})"""

    // limit, limitOpt, skip, skipOpt
    Venue
      .where(_.mayor eqs 1)
      .limit(10)
      .toString() shouldBe """db.venues.find({"mayor":1}).limit(10)"""
    Venue
      .where(_.mayor eqs 1)
      .limitOpt(Some(10))
      .toString() shouldBe """db.venues.find({"mayor":1}).limit(10)"""
    Venue.where(_.mayor eqs 1).limitOpt(None).toString() shouldBe """db.venues.find({"mayor":1})"""
    Venue
      .where(_.mayor eqs 1)
      .skip(10)
      .toString() shouldBe """db.venues.find({"mayor":1}).skip(10)"""
    Venue
      .where(_.mayor eqs 1)
      .skipOpt(Some(10))
      .toString() shouldBe """db.venues.find({"mayor":1}).skip(10)"""
    Venue.where(_.mayor eqs 1).skipOpt(None).toString() shouldBe """db.venues.find({"mayor":1})"""

    // raw query clauses
    Venue
      .where(_.mayor eqs 1)
      .raw(_ += "$where" -> BSONString("this.a > 3"))
      .toString() shouldBe """db.venues.find({"mayor":1,"$where":"this.a > 3"})"""

    // $not clauses
    Venue
      .where(_.mayor eqs 1)
      .not(_.mayor_count lt 5)
      .toString() shouldBe """db.venues.find({"mayor":1,"mayor_count":{"$not":{"$lt":5}}})"""
    Venue
      .where(_.mayor eqs 1)
      .not(_.mayor_count lt 5)
      .and(_.mayor_count gt 3)
      .toString() shouldBe """db.venues.find({"mayor":1,"mayor_count":{"$gt":3,"$not":{"$lt":5}}})"""
  }

  @Test
  def testModifyQueryShouldProduceACorrectJSONQueryString {
    val d1 = new DateTime(2010, 5, 1, 0, 0, 0, 0, DateTimeZone.UTC)

    val query = """db.venues.update({"legid":1},"""
    val suffix = ",false,false)"
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.venuename setTo "fshq")
      .toString() shouldBe query + """{"$set":{"venuename":"fshq"}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.mayor_count setTo 3)
      .toString() shouldBe query + """{"$set":{"mayor_count":3}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.mayor_count unset)
      .toString() shouldBe query + """{"$unset":{"mayor_count":1}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.mayor_count setTo Some(3L))
      .toString() shouldBe query + """{"$set":{"mayor_count":3}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.mayor_count setTo None)
      .toString() shouldBe query + """{"$unset":{"mayor_count":1}}""" + suffix

    // Numeric
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.mayor_count inc 3)
      .toString() shouldBe query + """{"$inc":{"mayor_count":3}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.geolatlng.unsafeField[Double]("lat") inc 0.5)
      .toString() shouldBe query + """{"$inc":{"latlng.lat":0.5}}""" + suffix

    // Enumeration
    val query2 =
      """db.venueclaims.update({"uid":1},"""
    VenueClaim
      .where(_.userid eqs 1)
      .modify(_.status setTo ClaimStatus.approved)
      .toString() shouldBe query2 + """{"$set":{"status":"Approved"}}""" + suffix

    // Calendar
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.last_updated setTo d1)
      .toString() shouldBe query + """{"$set":{"last_updated":{"$date":1272672000000}}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.last_updated setTo d1.toDate)
      .toString() shouldBe query + """{"$set":{"last_updated":{"$date":1272672000000}}}""" + suffix

    // LatLong
    val ll = LatLong(37.4, -73.9)
    //    Venue.where(_.legacyid eqs 1).modify(_.geolatlng setTo ll).toString() shouldBe query + """{"$set":{"latlng":[37.4,-73.9]}}""" + suffix

    // Lists
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity setTo List(5))
      .toString() shouldBe query + """{"$set":{"popularity":[5]}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity push 5)
      .toString() shouldBe query + """{"$push":{"popularity":5}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.tags pushAll List("a", "b"))
      .toString() shouldBe query + """{"$pushAll":{"tags":["a","b"]}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.tags addToSet "a")
      .toString() shouldBe query + """{"$addToSet":{"tags":"a"}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity addToSet List(1L, 2L))
      .toString() shouldBe query + """{"$addToSet":{"popularity":{"$each":[1,2]}}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.tags popFirst)
      .toString() shouldBe query + """{"$pop":{"tags":-1}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.tags popLast)
      .toString() shouldBe query + """{"$pop":{"tags":1}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.tags pull "a")
      .toString() shouldBe query + """{"$pull":{"tags":"a"}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity pullAll List(2L, 3L))
      .toString() shouldBe query + """{"$pullAll":{"popularity":[2,3]}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity at 0 inc 1)
      .toString() shouldBe query + """{"$inc":{"popularity.0":1}}""" + suffix
    // alternative syntax
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity idx 0 inc 1)
      .toString() shouldBe query + """{"$inc":{"popularity.0":1}}""" + suffix

    // Enumeration list
    //    OAuthConsumer.modify(_.privileges addToSet ConsumerPrivilege.awardBadges).toString() shouldBe """db.oauthconsumers.update({},{"$addToSet":{"privileges":"Award badges"}}""" + suffix

    // BsonRecordField and BsonRecordListField with nested Enumeration
    val claims = List(
      VenueClaimBson.createRecord.userid(1).status(ClaimStatus.approved).date(d1.toDate))
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.claims setTo claims)
      .toString() shouldBe query + """{"$set":{"claims":[{"status":"Approved","uid":1,"source":{},"date":{"$date":1272672000000}}]}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.lastClaim setTo claims.head)
      .toString() shouldBe query + """{"$set":{"lastClaim":{"status":"Approved","uid":1,"source":{},"date":{"$date":1272672000000}}}}""" + suffix

    // Map
    val m = Map("foo" -> 1L)
    val query3 = """db.tips.update({"legid":1},"""
    Tip
      .where(_.legacyid eqs 1)
      .modify(_.counts setTo m)
      .toString() shouldBe query3 + """{"$set":{"counts":{"foo":1}}}""" + suffix
    Tip
      .where(_.legacyid eqs 1)
      .modify(_.counts at "foo" setTo 3)
      .toString() shouldBe query3 + """{"$set":{"counts.foo":3}}""" + suffix
    Tip
      .where(_.legacyid eqs 1)
      .modify(_.counts at "foo" inc 5)
      .toString() shouldBe query3 + """{"$inc":{"counts.foo":5}}""" + suffix
    Tip
      .where(_.legacyid eqs 1)
      .modify(_.counts at "foo" unset)
      .toString() shouldBe query3 + """{"$unset":{"counts.foo":1}}""" + suffix
    Tip
      .where(_.legacyid eqs 1)
      .modify(_.counts setTo Map("foo" -> 3L, "bar" -> 5L))
      .toString() shouldBe query3 + """{"$set":{"counts":{"foo":3,"bar":5}}}""" + suffix

    // Multiple updates
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.venuename setTo "fshq")
      .and(_.mayor_count setTo 3)
      .toString() shouldBe query + """{"$set":{"mayor_count":3,"venuename":"fshq"}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.venuename setTo "fshq")
      .and(_.mayor_count inc 1)
      .toString() shouldBe query + """{"$set":{"venuename":"fshq"},"$inc":{"mayor_count":1}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.venuename setTo "fshq")
      .and(_.mayor_count setTo 3)
      .and(_.mayor_count inc 1)
      .toString() shouldBe query + """{"$set":{"mayor_count":3,"venuename":"fshq"},"$inc":{"mayor_count":1}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity addToSet 3)
      .and(_.tags addToSet List("a", "b"))
      .toString() shouldBe query + """{"$addToSet":{"tags":{"$each":["a","b"]},"popularity":3}}""" + suffix

    // Noop query
    Venue.where(_.legacyid eqs 1).noop().toString() shouldBe query + "{}" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .noop()
      .modify(_.venuename setTo "fshq")
      .toString() shouldBe query + """{"$set":{"venuename":"fshq"}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .noop()
      .and(_.venuename setTo "fshq")
      .toString() shouldBe query + """{"$set":{"venuename":"fshq"}}""" + suffix

    // $bit
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.mayor_count bitAnd 3)
      .toString() shouldBe query + """{"$bit":{"mayor_count":{"and":3}}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.mayor_count bitOr 3)
      .toString() shouldBe query + """{"$bit":{"mayor_count":{"or":3}}}""" + suffix

    // $rename
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.venuename rename "vn")
      .toString() shouldBe query + """{"$rename":{"venuename":"vn"}}""" + suffix

    // pullWhere
    /*
    object tags extends MongoListField[Venue, String](this)
    object popularity extends MongoListField[Venue, Long](this)
    object categories extends MongoListField[Venue, ObjectId](this)
    object claims extends BsonRecordListField(this, VenueClaimBson)
     */
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.tags pullWhere (_ startsWith "prefix"))
      .toString() shouldBe query + """{"$pull":{"tags":{"$regex":"^\\Qprefix\\E"}}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity pullWhere (_ gt 2))
      .toString() shouldBe query + """{"$pull":{"popularity":{"$gt":2}}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.popularity pullWhere (_ gt 2, _ lt 5))
      .toString() shouldBe query + """{"$pull":{"popularity":{"$gt":2,"$lt":5}}}""" + suffix
    Venue
      .where(_.legacyid eqs 1)
      .modify(_.claims pullObjectWhere (_.status eqs ClaimStatus.approved,
      _.userid eqs 2097))
      .toString() shouldBe query + """{"$pull":{"claims":{"uid":2097,"status":"Approved"}}}""" + suffix
  }

  @Test
  def testProduceACorrectSignatureString {
    val d1 = new DateTime(2010, 5, 1, 0, 0, 0, 0, DateTimeZone.UTC)
    val d2 = new DateTime(2010, 5, 2, 0, 0, 0, 0, DateTimeZone.UTC)
    val oid = BSONObjectID.generate

    // basic ops
    Venue.where(_.mayor eqs 1).signature() shouldBe """db.venues.find({"mayor":0})"""
    Venue
      .where(_.venuename eqs "Starbucks")
      .signature() shouldBe """db.venues.find({"venuename":0})"""
    Venue.where(_.closed eqs true).signature() shouldBe """db.venues.find({"closed":0})"""
    Venue.where(_.id eqs oid).signature() shouldBe """db.venues.find({"_id":0})"""
    VenueClaim
      .where(_.status eqs ClaimStatus.approved)
      .signature() shouldBe """db.venueclaims.find({"status":0})"""
    Venue
      .where(_.mayor_count gte 5)
      .signature() shouldBe """db.venues.find({"mayor_count":{"$gte":0}})"""
    VenueClaim
      .where(_.status neqs ClaimStatus.approved)
      .signature() shouldBe """db.venueclaims.find({"status":{"$ne":0}})"""
    Venue
      .where(_.legacyid in List(123L, 456L))
      .signature() shouldBe """db.venues.find({"legid":{"$in":0}})"""
    Venue.where(_.id exists true).signature() shouldBe """db.venues.find({"_id":{"$exists":0}})"""
    Venue
      .where(_.venuename startsWith "Starbucks")
      .signature() shouldBe """db.venues.find({"venuename":{"$regex":0,"$options":0}})"""

    // list
    Venue
      .where(_.tags all List("db", "ka"))
      .signature() shouldBe """db.venues.find({"tags":{"$all":0}})"""
    Venue
      .where(_.tags in List("db", "ka"))
      .signature() shouldBe """db.venues.find({"tags":{"$in":0}})"""
    Venue.where(_.tags size 3).signature() shouldBe """db.venues.find({"tags":{"$size":0}})"""
    Venue.where(_.tags contains "karaoke").signature() shouldBe """db.venues.find({"tags":0})"""
    Venue
      .where(_.popularity contains 3)
      .signature() shouldBe """db.venues.find({"popularity":0})"""
    Venue
      .where(_.popularity at 0 eqs 3)
      .signature() shouldBe """db.venues.find({"popularity.0":0})"""
    Venue
      .where(_.categories at 0 eqs oid)
      .signature() shouldBe """db.venues.find({"categories.0":0})"""
    Venue
      .where(_.tags at 0 startsWith "kara")
      .signature() shouldBe """db.venues.find({"tags.0":{"$regex":0,"$options":0}})"""
    Venue
      .where(_.tags idx 0 startsWith "kara")
      .signature() shouldBe """db.venues.find({"tags.0":{"$regex":0,"$options":0}})"""

    // map
    Tip.where(_.counts at "foo" eqs 3).signature() shouldBe """db.tips.find({"counts.foo":0})"""

    // near
    Venue
      .where(_.geolatlng near (39.0, -74.0, Degrees(0.2)))
      .signature() shouldBe """db.venues.find({"latlng":{"$near":0}})"""
    Venue
      .where(_.geolatlng withinCircle (1.0, 2.0, Degrees(0.3)))
      .signature() shouldBe """db.venues.find({"latlng":{"$within":{"$center":0}}})"""
    Venue
      .where(_.geolatlng withinBox (1.0, 2.0, 3.0, 4.0))
      .signature() shouldBe """db.venues.find({"latlng":{"$within":{"$box":0}}})"""
    Venue
      .where(_.geolatlng eqs (45.0, 50.0))
      .signature() shouldBe """db.venues.find({"latlng":0})"""
    Venue
      .where(_.geolatlng nearSphere (39.0, -74.0, Radians(1.0)))
      .signature() shouldBe """db.venues.find({"latlng":{"$nearSphere":0,"$maxDistance":0}})"""

    // id, date range
    Venue.where(_.id before d2).signature() shouldBe """db.venues.find({"_id":{"$lt":0}})"""
    Venue
      .where(_.last_updated before d2)
      .signature() shouldBe """db.venues.find({"last_updated":{"$lt":0}})"""

    // Case class list field
    Comment
      .where(_.comments.unsafeField[Int]("z") contains 123)
      .signature() shouldBe """db.comments.find({"comments.z":0})"""
    Comment
      .where(_.comments.unsafeField[String]("comment") contains "hi")
      .signature() shouldBe """db.comments.find({"comments.comment":0})"""

    // Enumeration list
    //    OAuthConsumer.where(_.privileges contains ConsumerPrivilege.awardBadges).signature() shouldBe """db.oauthconsumers.find({"privileges":0})"""
    //    OAuthConsumer.where(_.privileges at 0 eqs ConsumerPrivilege.awardBadges).signature() shouldBe """db.oauthconsumers.find({"privileges.0":0})"""

    // Field type
    Venue
      .where(_.legacyid hastype MongoType.String)
      .signature() shouldBe """db.venues.find({"legid":{"$type":0}})"""

    // Modulus
    Venue
      .where(_.legacyid mod (5, 1))
      .signature() shouldBe """db.venues.find({"legid":{"$mod":0}})"""

    // compound queries
    Venue
      .where(_.mayor eqs 1)
      .and(_.tags contains "karaoke")
      .signature() shouldBe """db.venues.find({"mayor":0,"tags":0})"""
    Venue
      .where(_.mayor eqs 1)
      .and(_.mayor_count gt 3)
      .and(_.mayor_count lt 5)
      .signature() shouldBe """db.venues.find({"mayor":0,"mayor_count":{"$lt":0,"$gt":0}})"""

    // queries with no clauses
    metaRecordToQueryBuilder(Venue).signature() shouldBe "db.venues.find({})"
    Venue.orderDesc(_.id).signature() shouldBe """db.venues.find({}).sort({"_id":-1})"""

    // ordered queries
    Venue
      .where(_.mayor eqs 1)
      .orderAsc(_.legacyid)
      .signature() shouldBe """db.venues.find({"mayor":0}).sort({"legid":1})"""
    Venue
      .where(_.mayor eqs 1)
      .orderDesc(_.legacyid)
      .andAsc(_.userid)
      .signature() shouldBe """db.venues.find({"mayor":0}).sort({"legid":-1,"userid":1})"""

    // select queries
    Venue
      .where(_.mayor eqs 1)
      .select(_.legacyid)
      .signature() shouldBe """db.venues.find({"mayor":0})"""

    // Scan should be the same as and/where
    Venue
      .where(_.mayor eqs 1)
      .scan(_.tags contains "karaoke")
      .signature() shouldBe """db.venues.find({"mayor":0,"tags":0})"""

    // or queries
    Venue
      .where(_.mayor eqs 1)
      .or(_.where(_.id eqs oid))
      .signature() shouldBe """db.venues.find({"mayor":0,"$or":[{"_id":0}]})"""
  }

  @Test
  def testFindAndModifyQueryShouldProduceACorrectJSONQueryString {
    Venue
      .where(_.legacyid eqs 1)
      .findAndModify(_.venuename setTo "fshq")
      .toString()
      .shouldBe(
        """db.venues.findAndModify({query:{"legid":1},update:{"$set":{"venuename":"fshq"}},new:false,upsert:false})""")
    Venue
      .where(_.legacyid eqs 1)
      .orderAsc(_.popularity)
      .findAndModify(_.venuename setTo "fshq")
      .toString()
      .shouldBe(
        """db.venues.findAndModify({query:{"legid":1},sort:{"popularity":1},update:{"$set":{"venuename":"fshq"}},new:false,upsert:false})""")
    Venue
      .where(_.legacyid eqs 1)
      .select(_.mayor, _.closed)
      .findAndModify(_.venuename setTo "fshq")
      .toString()
      .shouldBe(
        """db.venues.findAndModify({query:{"legid":1},update:{"$set":{"venuename":"fshq"}},new:false,fields:{"mayor":1,"closed":1},upsert:false})""")
  }

  @Test
  def testOrQueryShouldProduceACorrectJSONQueryString {
    // Simple $or
    Venue
      .or(_.where(_.legacyid eqs 1), _.where(_.mayor eqs 2))
      .toString() shouldBe
      """db.venues.find({"$or":[{"legid":1},{"mayor":2}]})"""

    // Compound $or
    Venue
      .where(_.tags size 0)
      .or(_.where(_.legacyid eqs 1), _.where(_.mayor eqs 2))
      .toString() shouldBe
      """db.venues.find({"tags":{"$size":0},"$or":[{"legid":1},{"mayor":2}]})"""

    // $or with additional "and" clauses
    Venue
      .where(_.tags size 0)
      .or(_.where(_.legacyid eqs 1).and(_.closed eqs true), _.where(_.mayor eqs 2))
      .toString() shouldBe
      """db.venues.find({"tags":{"$size":0},"$or":[{"legid":1,"closed":true},{"mayor":2}]})"""

    // Nested $or
    Venue
      .or(_.where(_.legacyid eqs 1)
            .or(_.where(_.closed eqs true), _.where(_.closed exists false)),
          _.where(_.mayor eqs 2))
      .toString() shouldBe
      """db.venues.find({"$or":[{"legid":1,"$or":[{"closed":true},{"closed":{"$exists":false}}]},{"mayor":2}]})"""

    // $or with modify
    Venue
      .or(_.where(_.legacyid eqs 1), _.where(_.mayor eqs 2))
      .modify(_.userid setTo 1)
      .toString() shouldBe
      """db.venues.update({"$or":[{"legid":1},{"mayor":2}]},{"$set":{"userid":1}},false,false)"""

    // $or with optional where clause
    Venue
      .or(_.where(_.legacyid eqs 1), _.whereOpt(None)(_.mayor eqs _))
      .modify(_.userid setTo 1)
      .toString() shouldBe
      """db.venues.update({"$or":[{"legid":1}]},{"$set":{"userid":1}},false,false)"""

    Venue
      .or(_.where(_.legacyid eqs 1), _.whereOpt(Some(2))(_.mayor eqs _))
      .modify(_.userid setTo 1)
      .toString() shouldBe
      """db.venues.update({"$or":[{"legid":1},{"mayor":2}]},{"$set":{"userid":1}},false,false)"""

    // OrQuery syntax
    val q1 = Venue.where(_.legacyid eqs 1)
    val q2 = Venue.where(_.legacyid eqs 2)
    OrQuery(q1, q2).toString() shouldBe
      """db.venues.find({"$or":[{"legid":1},{"legid":2}]})"""
    OrQuery(q1, q2).and(_.mayor eqs 0).toString() shouldBe
      """db.venues.find({"mayor":0,"$or":[{"legid":1},{"legid":2}]})"""
    OrQuery(q1, q2.or(_.where(_.closed eqs true), _.where(_.closed exists false)))
      .toString() shouldBe
      """db.venues.find({"$or":[{"legid":1},{"legid":2,"$or":[{"closed":true},{"closed":{"$exists":false}}]}]})"""
  }

  //  @Test
  //  def testHints {
  //    Venue.where(_.legacyid eqs 1).hint(Venue.idIdx).toString()        shouldBe """db.venues.find({"legid":1}).hint({"_id":1})"""
  //    Venue.where(_.legacyid eqs 1).hint(Venue.legIdx).toString()       shouldBe """db.venues.find({"legid":1}).hint({"legid":-1})"""
  //    Venue.where(_.legacyid eqs 1).hint(Venue.geoIdx).toString()       shouldBe """db.venues.find({"legid":1}).hint({"latlng":"2d"})"""
  //    Venue.where(_.legacyid eqs 1).hint(Venue.geoCustomIdx).toString() shouldBe """db.venues.find({"legid":1}).hint({"latlng":"custom","tags":1})"""
  //  }

  @Test
  def testDollarSelector {
    Venue
      .where(_.legacyid eqs 1)
      .and(_.claims.subfield(_.userid) contains 2)
      .modify(_.claims.$.subfield(_.status) setTo ClaimStatus.approved)
      .toString() shouldBe
      """db.venues.update({"legid":1,"claims.uid":2},{"$set":{"claims.$.status":"Approved"}},false,false)"""

    Venue
      .where(_.legacyid eqs 1)
      .and(_.tags contains "sometag")
      .modify(_.tags.$ setTo "othertag")
      .toString() shouldBe
      """db.venues.update({"legid":1,"tags":"sometag"},{"$set":{"tags.$":"othertag"}},false,false)"""

    Venue
      .where(_.legacyid eqs 1)
      .and(_.tags contains "sometag")
      .select(_.tags.$)
      .toString() shouldBe
      """db.venues.find({"legid":1,"tags":"sometag"},{"tags.$":1})"""
  }

  @Test
  def testWhereOpt {
    val someId = Some(1L)
    val noId: Option[Long] = None
    val someList = Some(List(1L, 2L))
    val noList: Option[List[Long]] = None

    // whereOpt
    Venue.whereOpt(someId)(_.legacyid eqs _).toString() shouldBe """db.venues.find({"legid":1})"""
    Venue.whereOpt(noId)(_.legacyid eqs _).toString() shouldBe """db.venues.find({})"""
    Venue
      .whereOpt(someId)(_.legacyid eqs _)
      .and(_.mayor eqs 2)
      .toString() shouldBe """db.venues.find({"legid":1,"mayor":2})"""
    Venue
      .whereOpt(noId)(_.legacyid eqs _)
      .and(_.mayor eqs 2)
      .toString() shouldBe """db.venues.find({"mayor":2})"""

    // whereOpt: lists
    Venue
      .whereOpt(someList)(_.legacyid in _)
      .toString() shouldBe """db.venues.find({"legid":{"$in":[1,2]}})"""
    Venue.whereOpt(noList)(_.legacyid in _).toString() shouldBe """db.venues.find({})"""

    // whereOpt: enum
    val someEnum = Some(VenueStatus.open)
    val noEnum: Option[VenueStatus.type#Value] = None
    Venue
      .whereOpt(someEnum)(_.status eqs _)
      .toString() shouldBe """db.venues.find({"status":"Open"})"""
    Venue.whereOpt(noEnum)(_.status eqs _).toString() shouldBe """db.venues.find({})"""

    // whereOpt: date
    val someDate = Some(new DateTime(2010, 5, 1, 0, 0, 0, 0, DateTimeZone.UTC))
    val noDate: Option[DateTime] = None
    Venue
      .whereOpt(someDate)(_.last_updated after _)
      .toString() shouldBe """db.venues.find({"last_updated":{"$gt":{"$date":1272672000000}}})"""
    Venue.whereOpt(noDate)(_.last_updated after _).toString() shouldBe """db.venues.find({})"""

    // andOpt
    Venue
      .where(_.mayor eqs 2)
      .andOpt(someId)(_.legacyid eqs _)
      .toString() shouldBe """db.venues.find({"mayor":2,"legid":1})"""
    Venue
      .where(_.mayor eqs 2)
      .andOpt(noId)(_.legacyid eqs _)
      .toString() shouldBe """db.venues.find({"mayor":2})"""

    // scanOpt
    Venue.scanOpt(someId)(_.legacyid eqs _).toString() shouldBe """db.venues.find({"legid":1})"""
    Venue.scanOpt(noId)(_.legacyid eqs _).toString() shouldBe """db.venues.find({})"""
    Venue
      .scanOpt(someId)(_.legacyid eqs _)
      .and(_.mayor eqs 2)
      .toString() shouldBe """db.venues.find({"legid":1,"mayor":2})"""
    Venue
      .scanOpt(noId)(_.legacyid eqs _)
      .and(_.mayor eqs 2)
      .toString() shouldBe """db.venues.find({"mayor":2})"""

    // iscanOpt
    Venue.iscanOpt(someId)(_.legacyid eqs _).toString() shouldBe """db.venues.find({"legid":1})"""
    Venue.iscanOpt(noId)(_.legacyid eqs _).toString() shouldBe """db.venues.find({})"""
    Venue
      .iscanOpt(someId)(_.legacyid eqs _)
      .and(_.mayor eqs 2)
      .toString() shouldBe """db.venues.find({"legid":1,"mayor":2})"""
    Venue
      .iscanOpt(noId)(_.legacyid eqs _)
      .and(_.mayor eqs 2)
      .toString() shouldBe """db.venues.find({"mayor":2})"""

    // modify
    val q = Venue.where(_.legacyid eqs 1)
    val prefix = """db.venues.update({"legid":1},"""
    val suffix = ",false,false)"

    q.modifyOpt(someId)(_.legacyid setTo _)
      .toString() shouldBe prefix + """{"$set":{"legid":1}}""" + suffix
    q.modifyOpt(noId)(_.legacyid setTo _).toString() shouldBe prefix + """{}""" + suffix
    q.modifyOpt(someEnum)(_.status setTo _)
      .toString() shouldBe prefix + """{"$set":{"status":"Open"}}""" + suffix
    q.modifyOpt(noEnum)(_.status setTo _).toString() shouldBe prefix + """{}""" + suffix
  }

  @Test
  def testShardKey {
    Like.where(_.checkin eqs 123).toString() shouldBe """db.likes.find({"checkin":123})"""
    Like.where(_.userid eqs 123).toString() shouldBe """db.likes.find({"userid":123})"""
    Like.where(_.userid eqs 123).allShards.toString() shouldBe """db.likes.find({"userid":123})"""
    Like
      .where(_.userid eqs 123)
      .allShards
      .noop()
      .toString() shouldBe """db.likes.update({"userid":123},{},false,false)"""
    //    Like.withShardKey(_.userid eqs 123).toString() shouldBe """db.likes.find({"userid":123})"""
    //    Like.withShardKey(_.userid in List(123L, 456L)).toString() shouldBe """db.likes.find({"userid":{"$in":[123,456]}})"""
    //    Like.withShardKey(_.userid eqs 123).and(_.checkin eqs 1).toString() shouldBe """db.likes.find({"userid":123,"checkin":1})"""
    //    Like.where(_.checkin eqs 1).withShardKey(_.userid eqs 123).toString() shouldBe """db.likes.find({"checkin":1,"userid":123})"""
  }

  @Test
  def testCommonSuperclassForPhantomTypes {
    def maybeLimit(legid: Long, limitOpt: Option[Int]) = {
      limitOpt match {
        case Some(limit) => Venue.where(_.legacyid eqs legid).limit(limit)
        case None => Venue.where(_.legacyid eqs legid)
      }
    }

    maybeLimit(1, None).toString() shouldBe """db.venues.find({"legid":1})"""
    maybeLimit(1, Some(5)).toString() shouldBe """db.venues.find({"legid":1}).limit(5)"""
  }

  //  @Test
  //  def testSetReadPreference: Unit = {
  //    type Q = Query[Venue, Venue, _]
  //
  //    Venue.where(_.mayor eqs 2).asInstanceOf[Q].readPreference shouldBe None
  //    Venue.where(_.mayor eqs 2).setReadPreference(ReadPreference.secondary).asInstanceOf[Q].readPreference shouldBe Some(ReadPreference.secondary)
  //    Venue.where(_.mayor eqs 2).setReadPreference(ReadPreference.primary).asInstanceOf[Q].readPreference shouldBe Some(ReadPreference.primary)
  //    Venue.where(_.mayor eqs 2).setReadPreference(ReadPreference.secondary).setReadPreference(ReadPreference.primary).asInstanceOf[Q].readPreference shouldBe Some(ReadPreference.primary)
  //  }

  @Test
  def testQueryOptimizerDetectsEmptyQueries: Unit = {
    val optimizer = new QueryOptimizer

    optimizer.isEmptyQuery(Venue.where(_.mayor eqs 1)) shouldBe false
    optimizer.isEmptyQuery(Venue.where(_.mayor in List())) shouldBe true
    optimizer.isEmptyQuery(Venue.where(_.tags in List())) shouldBe true
    optimizer.isEmptyQuery(Venue.where(_.tags all List())) shouldBe true
    optimizer.isEmptyQuery(Venue.where(_.tags contains "karaoke").and(_.mayor in List())) shouldBe true
    optimizer.isEmptyQuery(Venue.where(_.mayor in List()).and(_.tags contains "karaoke")) shouldBe true
    optimizer.isEmptyQuery(Comment.where(_.comments in List())) shouldBe true
    optimizer.isEmptyQuery(Venue.where(_.mayor in List()).scan(_.mayor_count eqs 5)) shouldBe true
    optimizer.isEmptyQuery(Venue.where(_.mayor eqs 1).modify(_.venuename setTo "fshq")) shouldBe false
    optimizer.isEmptyQuery(Venue.where(_.mayor in List()).modify(_.venuename setTo "fshq")) shouldBe true
  }

}
