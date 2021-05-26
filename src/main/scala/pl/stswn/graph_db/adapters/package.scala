package pl.stswn.graph_db

import zio.Has

package object adapters {
  type TestAdapter = Has[TestAdapter.Service]
}
