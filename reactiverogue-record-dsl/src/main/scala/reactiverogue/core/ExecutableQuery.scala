package reactiverogue.core

import play.api.libs.iteratee.Enumerator
import reactivemongo.api.commands.{WriteConcern, WriteResult}
import reactiverogue.core.MongoHelpers.MongoSelect
import reactiverogue.db.MongoResolution

import scala.concurrent.{ExecutionContext, Future}

case class ExecutableQuery[MB, M <: MB, R, State](
    query: Query[M, R, State],
    db: QueryExecutor[MB])(implicit ev: ShardingOk[M, State]) {

  /**
    * Gets the size of the query result. This should only be called on queries that do not
    * have limits or skips.
    */
  def count()(implicit ec: ExecutionContext, _res: MongoResolution): Future[Int] =
    db.count(query)

  //  /**
  //   * Returns the number of distinct values returned by a query. The query must not have
  //   * limit or skip clauses.
  //   */
  //  def countDistinct[V](field: M => Field[V, _]): Long =
  //    db.countDistinct(query)(field.asInstanceOf[M => Field[V, M]])
  //
  /**
    * Returns a list of distinct values returned by a query. The query must not have
    * limit or skip clauses.
    */
  def distinct[V](field: M => SelectField[V, _])(implicit ec: ExecutionContext,
                                                 _res: MongoResolution): Future[List[V]] = {
    db.distinct(query)(field)
  }

  /**
    * Checks if there are any records that match this query.
    */
  def exists()(implicit ev: State <:< Unlimited with Unskipped,
               ec: ExecutionContext,
               _res: MongoResolution): Future[Boolean] = {
    val q = query.copy(select = Some(MongoSelect[M, Null](Nil, _ => null)))
    db.fetchOne(q).map(_.isDefined)
  }

  /**
    * Executes a function on each record value returned by a query.
    * @param f a function to be invoked on each fetched record.
    * @return nothing.
    */
  def foreach(f: R => Unit)(implicit ec: ExecutionContext, _res: MongoResolution): Unit =
    db.foreach(query)(f)

  /**
    * Execute the query, returning all of the records that match the query.
    * @return a list containing the records that match the query
    */
  def fetch()(implicit ec: ExecutionContext, _res: MongoResolution): Future[List[R]] =
    db.fetch(query)

  /**
    * Execute a query, returning no more than a specified number of result records. The
    * query must not have a limit clause.
    * @param limit the maximum number of records to return.
    */
  def fetch[S2](limit: Int)(implicit ev1: AddLimit[State, S2],
                            ev2: ShardingOk[M, S2],
                            ec: ExecutionContext,
                            _res: MongoResolution): Future[List[R]] =
    db.fetch(query.limit(limit))

  def fetchEnumerator()(implicit ec: ExecutionContext, _res: MongoResolution): Enumerator[R] =
    db.fetchEnumerator(query)

  //  /**
  //   * fetch a batch of results, and execute a function on each element of the list.
  //   * @param f the function to invoke on the records that match the query.
  //   * @return a list containing the results of invoking the function on each record.
  //   */
  //  def fetchBatch[T](batchSize: Int)(f: List[R] => List[T]): List[T] =
  //    db.fetchBatch(query, batchSize)(f).toList

  /**
    * Fetches the first record that matches the query. The query must not contain a "limited" clause.
    * @return an option record containing either the first result that matches the
    *         query, or None if there are no records that match.
    */
  def get[S2]()(implicit ev1: AddLimit[State, S2],
                ev2: ShardingOk[M, S2],
                ec: ExecutionContext,
                _res: MongoResolution): Future[Option[R]] =
    db.fetchOne(query)

  /**
    * Fetches the records that match the query in paginated form. The query must not contain
    * a "limit" clause.
    * @param countPerPage the number of records to be contained in each page of the result.
    */
  def paginate(countPerPage: Int)(implicit ev1: Required[State, Unlimited with Unskipped],
                                  ev2: ShardingOk[M, State],
                                  ec: ExecutionContext,
                                  _res: MongoResolution) = {
    new PaginatedQuery(ev1(query), db, countPerPage)
  }

  /**
    * Delete all of the records that match the query. The query must not contain any "skip",
    * "limit", or "select" clauses. Sends the delete operation to mongo, and returns - does
    * <em>not</em> wait for the delete to be finished.
    */
  def bulkDelete_!!!()(implicit ev1: Required[State, Unselected with Unlimited with Unskipped],
                       ec: ExecutionContext,
                       _res: MongoResolution): Future[WriteResult] =
    db.bulkDelete_!!(query)

  /**
    * Delete all of the records that match the query. The query must not contain any "skip",
    * "limit", or "select" clauses. Sends the delete operation to mongo, and waits for the
    * delete operation to complete before returning to the caller.
    */
  def bulkDelete_!!(concern: WriteConcern)(
      implicit ev1: Required[State, Unselected with Unlimited with Unskipped],
      ec: ExecutionContext,
      _res: MongoResolution): Future[WriteResult] =
    db.bulkDelete_!!(query, concern)

  /**
    * Finds the first record that matches the query (if any), fetches it, and then deletes it.
    * A copy of the deleted record is returned to the caller.
    */
  def findAndDeleteOne()(implicit ev: RequireShardKey[M, State],
                         ec: ExecutionContext,
                         _res: MongoResolution): Future[Option[R]] =
    db.findAndDeleteOne(query)

  //  /**
  //   * Return a string containing details about how the query would be executed in mongo.
  //   * In particular, this is useful for finding out what indexes will be used by the query.
  //   */
  //  def explain(): String =
  //    db.explain(query)

  //  def iterate[S](state: S)(handler: (S, Iter.Event[R]) => Iter.Command[S]): S =
  //    db.iterate(query, state)(handler)
  //
  //  def iterateBatch[S](batchSize: Int, state: S)(handler: (S, Iter.Event[List[R]]) => Iter.Command[S]): S =
  //    db.iterateBatch(query, batchSize, state)(handler)
}

case class ExecutableModifyQuery[MB, M <: MB, State](query: ModifyQuery[M, State],
                                                     db: QueryExecutor[MB]) {
  def updateMulti()(implicit ec: ExecutionContext, _res: MongoResolution): Future[WriteResult] =
    db.updateMulti(query)

  def updateOne()(implicit ev: RequireShardKey[M, State],
                  ec: ExecutionContext,
                  _res: MongoResolution): Future[WriteResult] =
    db.updateOne(query)

  def upsertOne()(implicit ev: RequireShardKey[M, State],
                  ec: ExecutionContext,
                  _res: MongoResolution): Future[WriteResult] =
    db.upsertOne(query)

  def updateMulti(writeConcern: WriteConcern)(implicit ec: ExecutionContext,
                                              _res: MongoResolution): Future[WriteResult] =
    db.updateMulti(query, writeConcern)

  def updateOne(writeConcern: WriteConcern)(implicit ev: RequireShardKey[M, State],
                                            ec: ExecutionContext,
                                            _res: MongoResolution): Future[WriteResult] =
    db.updateOne(query, writeConcern)

  def upsertOne(writeConcern: WriteConcern)(implicit ev: RequireShardKey[M, State],
                                            ec: ExecutionContext,
                                            _res: MongoResolution): Future[WriteResult] =
    db.upsertOne(query, writeConcern)
}

case class ExecutableFindAndModifyQuery[MB, M <: MB, R](query: FindAndModifyQuery[M, R],
                                                        db: QueryExecutor[MB]) {
  def updateOne(returnNew: Boolean = false)(implicit ec: ExecutionContext,
                                            _res: MongoResolution): Future[Option[R]] =
    db.findAndUpdateOne(query, returnNew)

  def upsertOne(returnNew: Boolean = false)(implicit ec: ExecutionContext,
                                            _res: MongoResolution): Future[Option[R]] =
    db.findAndUpsertOne(query, returnNew)
}

class PaginatedQuery[MB, M <: MB, R, +State <: Unlimited with Unskipped](q: Query[M, R, State],
                                                                         db: QueryExecutor[MB],
                                                                         val countPerPage: Int,
                                                                         val pageNum: Int = 1)(
    implicit ev: ShardingOk[M, State],
    ec: ExecutionContext,
    _res: MongoResolution) {
  def copy() = new PaginatedQuery(q, db, countPerPage, pageNum)

  def setPage(p: Int) = if (p == pageNum) this else new PaginatedQuery(q, db, countPerPage, p)

  def setCountPerPage(c: Int) =
    if (c == countPerPage) this else new PaginatedQuery(q, db, c, pageNum)

  lazy val countAllF: Future[Int] = db.count(q)

  def fetch(): Future[List[R]] = {
    db.fetch(q.skip(countPerPage * (pageNum - 1)).limit(countPerPage))
  }

  def numPages: Future[Int] =
    countAllF.map { countAll =>
      math.ceil(countAll.toDouble / countPerPage.toDouble).toInt max 1
    }

}
