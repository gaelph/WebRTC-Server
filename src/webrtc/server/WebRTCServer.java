/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server;

import Database.*;
import STUN.StunServer;
import java.io.IOException;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.tls.DTLSTransport;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.json.simple.*;
import org.json.simple.parser.*;
import webrtc.server.HTTP.*;
import webrtc.server.WebRTC.*;
import webrtc.server.WebSocket.*;

/**
 *
 * @author gaelph
 */
public class WebRTCServer {

    public static final Integer WS_PORT = 3200;
    public static final Integer HTTP_PORT = 3201;
    public static final Integer UDP_PORT = 3208;

    private static WebSocketServer wsServer;

    private static Database db;

    private static final Logger LOG = Logger.getLogger(WebRTCServer.class.getName());

    public static void main(String[] args) {

        wsServer = WebSocketServer.createServer();

        db = new Database("127.0.0.1:3306", "webcamtest-db", "admin", "admin");

        db.getTable("rooms").findAll(row -> {
            try {
                wsServer.newRoom((String) row.get(RoomTable.RID), WebSocketRoom.TYPES.PERMANENT);
            }
            catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });

        HTTPServer httpServer = (HTTPServer) HTTPServer.createServer();
        httpServer.setRootPath("/Users/gaelph/NetBeansProjects/WebRTC Server/www/");

        httpServer.on("GET", "/api/user/:uid", (request, response) -> {

                  Table users = db.getTable("users");
                  JSONObject user = new JSONObject();
                  response.status = HTTPStatusCodes.NOT_FOUND;

                  users.find(UserTable.UID, request.params.get("uid"), row -> {
                         try {
                             user.put("sid", (String) row.get(UserTable.SID));
                             user.put("firstname", (String) row.get(UserTable.FIRST_NAME));
                             user.put("lastname", (String) row.get(UserTable.LAST_NAME));

                             response.status = HTTPStatusCodes.OK;
                             response.setBody(user.toJSONString());

                         }
                         catch (SQLException ex) {
                             LOG.log(Level.SEVERE, null, ex);
                         }
                     });
                  try {
                      response.send();
                  }
                  catch (Exception ex) {
                      LOG.log(Level.SEVERE, null, ex);
                  }
              });

        httpServer.on("POST", "/api/user/:uid", (request, response) -> {
                  UserTable users = UserTable.newInstance();

                  users.find(UserTable.UID, request.formContent.get("UID"), row -> {
                         try {
                             row.set(UserTable.EMAIL, request.formContent.get("email"));
                             row.set(UserTable.PASSWORD, request.formContent.get("password"));
                             row.set(UserTable.FIRST_NAME, request.formContent.get("firstName"));
                             row.set(UserTable.LAST_NAME, request.formContent.get("lastName"));

                             row.commit();

                             JSONObject jsonUser = new JSONObject();
                             jsonUser.put("UID", request.formContent.get("UID"));
                             jsonUser.put("firstname", request.formContent.get("firstName"));
                             jsonUser.put("lastname", request.formContent.get("lastName"));
                             response.setBody(jsonUser.toJSONString());

                             response.status = HTTPStatusCodes.OK;
                             response.send();
                         }
                         catch (SQLException ex) {
                             LOG.log(Level.SEVERE, null, ex);
                             response.status = HTTPStatusCodes.INTERNAL_SERVER_ERROR;
                             try {
                                 response.send();
                             }
                             catch (Exception ex1) {
                                 LOG.log(Level.SEVERE, null, ex1);
                             }
                         }
                         catch (Exception ex) {
                             LOG.log(Level.SEVERE, null, ex);
                         }
                     });
              });

        httpServer.on("DELETE", "/api/user/:uid", (request, response) -> {
                  UserTable users = UserTable.newInstance();

                  users.delete(UserTable.UID, request.params.get("UID"));

                  try {
                      response.send();
                  }
                  catch (Exception ex) {
                      LOG.log(Level.SEVERE, null, ex);
                  }
              });

        httpServer.on("GET", "/api/rooms", (request, response) -> {
                  JSONArray jRooms = new JSONArray();

                  wsServer.getRooms().stream()
                          .forEach((room) -> {
                              jRooms.add(room.toJSONString());
                          });

                  response.status = HTTPStatusCodes.OK;
                  response.setBody(jRooms.toJSONString());

                  try {
                      response.send();
                  }
                  catch (Exception ex) {
                      LOG.log(Level.SEVERE, null, ex);
                  }
              });

        httpServer.on("GET", "/api/room/:rid/users", (request, response) -> {

                  wsServer.getRooms().stream()
                          .filter((room) -> {
                              return room.rid.equals(request.params.get("rid"));
                          })
                          .findFirst().ifPresent((room) -> {
                              JSONArray users = new JSONArray();

                              room.participants
                                      .forEach((participant) -> {
                                          JSONObject jP = new JSONObject();

                                          db.getTable("users").find(UserTable.SID, participant.sid, row -> {
                                                                try {
                                                                    jP.put("firstname", row.get("first_name"));
                                                                    jP.put("sid", row.get("SID"));
                                                                }
                                                                catch (SQLException ex) {
                                                                    LOG.log(Level.SEVERE, null,
                                                                            ex);
                                                                }
                                                            });

                                          users.add(jP.toJSONString());
                                      });

                              response.status = HTTPStatusCodes.OK;
                              response.setContentType(HTTPMIMETypes.get(HTTPMIMETypes.TYPES.JSON) + "; charset=utf-8");
                              response.setBody(users.toJSONString());
                          });

                  try {
                      response.send();
                  }
                  catch (Exception ex) {
                      LOG.log(Level.SEVERE, null, ex);
                  }
              });

        httpServer.on("POST", "/api/login", (request, response) -> {
                  UserTable userTable = UserTable.newInstance();

                  Object[] params;

                  String contentType = request.header.get("content-type").split(";")[0].trim();

                  switch (contentType) {
                      case "application/json":
                          params = Arrays.asList(request.jsonContent.get("email"), request.jsonContent.get("password")).toArray();
                          break;

                      default:
                          response.status = 415;
                           {
                              try {
                                  response.send();
                              }
                              catch (Exception ex) {
                                  LOG.log(Level.SEVERE, null, ex);
                              }
                          }
                          return;
                  }

                  response.status = HTTPStatusCodes.UNAUTHORIZED;
                  response.setBody("No user with such email or password");

                  JSONObject message = new JSONObject();
                  JSONObject user = new JSONObject();

                  userTable.select(null, "`email`=? and `password`=?", params, null, false, row -> {
                               try {
                                   user.put("UID", row.get(UserTable.UID));
                                   user.put("SID", row.get(UserTable.SID)); //Is this relevant?
                                   user.put("firstname", row.get(UserTable.FIRST_NAME));
                                   user.put("lastname", row.get(UserTable.LAST_NAME));
                               }
                               catch (SQLException ex) {
                                   LOG.log(Level.SEVERE, null, ex);
                               }
                           });

                  if (!user.isEmpty()) {
                      message.put("user", user);
                      response.status = HTTPStatusCodes.OK;
                      response.setBody(message.toJSONString());

                      HTTPCookie cookie = new HTTPCookie("sid", (String) user.get("SID"), "127.0.0.1", "/");
                      response.cookies.add(cookie);
                      cookie = new HTTPCookie("sid", (String) user.get("SID"), "localhost", "/");
                      response.cookies.add(cookie);
                  }

                  try {
                      response.send();
                  }
                  catch (Exception ex) {
                      LOG.log(Level.SEVERE, null, ex);
                  }

              });

        httpServer.on("POST", "api/user/create", (request, response) -> {
                  UserTable users = UserTable.newInstance();

                  if (users.find(UserTable.EMAIL, request.formContent.get("email")).isPresent()) {
                      response.status = HTTPStatusCodes.BAD_REQUEST;
                      response.setBody("User with email already exists (" + (String) request.formContent.get("email") + ")");
                      try {
                          response.send();
                      }
                      catch (Exception ex) {
                          LOG.log(Level.SEVERE, null, ex);
                      }
                      return;
                  }

                  String UID = Utils.makeRandomString(24);
                  String apiToken = Utils.makeRandomString(32);

                  try {
                      users.newRow(row -> {
                          try {
                              row.set(UserTable.UID, UID);
                              row.set(UserTable.API_TOKEN, apiToken);
                              row.set(UserTable.EMAIL, request.formContent.get("email"));
                              row.set(UserTable.PASSWORD, request.formContent.get("password"));
                              row.set(UserTable.FIRST_NAME, request.formContent.get("firstName"));
                              row.set(UserTable.LAST_NAME, request.formContent.get("lastName"));
                              row.set(UserTable.STATUS, 0);

                              row.commit();
                          }
                          catch (SQLException ex) {
                              LOG.log(Level.SEVERE, null, ex);

                              response.status = HTTPStatusCodes.INTERNAL_SERVER_ERROR;
                              try {
                                  response.send();
                              }
                              catch (Exception ex1) {
                                  LOG.log(Level.SEVERE, null, ex1);
                              }
                          }
                      });
                  }
                  catch (SQLException ex) {
                      LOG.log(Level.SEVERE, null, ex);

                      response.status = HTTPStatusCodes.INTERNAL_SERVER_ERROR;
                      try {
                          response.send();
                      }
                      catch (Exception ex1) {
                          LOG.log(Level.SEVERE, null, ex1);
                      }

                      return;
                  }

                  users.find(UserTable.UID, UID, row -> {
                         try {
                             JSONObject user = new JSONObject();

                             user.put("UID", row.get(UserTable.UID));
                             user.put("api_token", apiToken);
                             user.put("SID", row.get(UserTable.SID)); //Is this relevant?
                             user.put("firstname", row.get(UserTable.FIRST_NAME));
                             user.put("lastname", row.get(UserTable.LAST_NAME));

                             response.status = HTTPStatusCodes.CREATED;
                             response.setBody(user.toJSONString());

                             response.send();

                         }
                         catch (SQLException ex) {
                             LOG.log(Level.SEVERE, null, ex);
                             response.status = HTTPStatusCodes.INTERNAL_SERVER_ERROR;
                             try {
                                 response.send();
                             }
                             catch (Exception ex1) {
                                 LOG.log(Level.SEVERE, null, ex1);
                             }
                         }
                         catch (Exception ex) {
                             LOG.log(Level.SEVERE, null, ex);
                         }
                     });
              });

        httpServer.listen(HTTP_PORT);

        wsServer.on("connect", (WebSocketConnection connection) -> {
                if (connection.request.cookies.stream()
                        .anyMatch(cookie -> cookie.name.equals("token"))) {
                    String UID = connection.request.cookies.stream().filter(cookie -> cookie.name.equals("token")).findFirst().get().value;

                    if (!db.getTable("users").find(UserTable.UID, UID).isPresent()) {
                        try {
                            connection.close();
                        }
                        catch (IOException ex) {
                            LOG.log(Level.SEVERE, null, ex);
                        }
                        return;
                    }

                    db.getTable("users")
                            .find(UserTable.UID, UID, row -> {
                              try {
                                  row.set(UserTable.SID, connection.sid);
                                  row.commit();
                                  LOG.log(Level.INFO, "User {0} connected", row.get(
                                          UserTable.UID) + ", " + row.get(
                                                  UserTable.FIRST_NAME));
                              }
                              catch (SQLException ex) {
                                  LOG.log(Level.SEVERE, null, ex);
                              }

                              connection.uid = UID;

                          });

                }

                PeerConnection pConnection = new PeerConnection();

                pConnection.on("icecandidate", object -> connection.emit("icecandidate", (String) object));

                connection.on("joined", rid -> {
                          connection.getJoinedRooms().forEach(room -> {
                              JSONObject msg = new JSONObject();
                              msg.put("sid", connection.sid);

                              db.getTable("users")
                                      .find(UserTable.SID, connection.sid, row -> {
                                        try {
                                            msg.put("firstname", row.get(UserTable.FIRST_NAME));

                                            LOG.log(Level.INFO, "User {0}",
                                                    row
                                                    .get(
                                                            UserTable.SID) + " joined room " + rid);
                                        }
                                        catch (SQLException ex) {
                                            LOG.log(Level.SEVERE, null, ex);
                                        }
                                    });

                              room.emit("user_joined", msg.toJSONString());
                          });
                      });

                connection.on("left", rid -> {
                          connection.getJoinedRooms().forEach(room -> {
                              JSONObject msg = new JSONObject();
                              msg.put("sid", connection.sid);

                              LOG.log(Level.INFO, "User {0}",
                                      connection.sid + " left room " + rid);

                              room.emit("user_left", msg.toJSONString());
                          });
                      });

                connection.on("disconnect", data -> {
                          wsServer.getRooms().stream().filter(room -> room.participants.contains(connection))
                                  .forEach(room -> room.leave(connection));

                          db.getTable("users")
                                  .find(UserTable.SID, connection.sid, row -> {
                                    try {
                                        LOG.log(Level.INFO, "User {0} disconnected",
                                                row
                                                .get(
                                                        UserTable.UID) + ", " + row
                                                .get(
                                                        UserTable.FIRST_NAME));
                                    }
                                    catch (SQLException ex) {
                                        LOG.log(Level.SEVERE, null, ex);
                                    }
                                });

                      });

                connection.on("message", data -> {

                      });

                connection.on("offer", data -> {
                          try {
                              JSONParser parser = new JSONParser();
                              String string = (String) parser.parse(data);
                              JSONObject offer = (JSONObject) parser.parse(string);

                              JSONObject body = (JSONObject) ((JSONObject) offer).get("body");

                              pConnection.setRemoteDescription(body);

                              body = pConnection.createAnswer();
                              body.put("sid", connection.sid);

                              connection.emit("answer", body.toJSONString());

                              pConnection.setLocalDescription(body);
                          }
                          catch (ParseException ex) {
                              LOG.log(Level.SEVERE, null, ex);
                          }
                          catch (Exception ex) {
                              Logger.getLogger(WebRTCServer.class.getName()).log(Level.SEVERE, null, ex);
                          }
                      });

                connection.on("answer", data -> {
                          try {
                              JSONParser parser = new JSONParser();
                              String string = (String) parser.parse(data);
                              JSONObject offer = (JSONObject) parser.parse(string);

                              JSONObject body = (JSONObject) ((JSONObject) offer).get("body");

                              pConnection.setRemoteDescription(body);
                          }
                          catch (ParseException ex) {
                              LOG.log(Level.SEVERE, null, ex);
                          }
                      });

                connection.on("icecandidate", (String data) -> {
                          try {
                              JSONParser parser = new JSONParser();
                              JSONObject message = (JSONObject) parser.parse((String) parser.parse(data));
                              JSONObject candidateDict = (JSONObject) message.get("body");
                              pConnection.addCandidate(candidateDict);
                          }
                          catch (ParseException ex) {
                              LOG.log(Level.SEVERE, null, ex);
                          }
                      });

                connection.on("hangup", data -> {
                          try {
                              JSONObject offer = (JSONObject) new JSONParser().parse(data);
                          }
                          catch (ParseException ex) {
                              LOG.log(Level.SEVERE, null, ex);
                          }
                      });
            });

        wsServer.listen(WS_PORT);

    }

}
