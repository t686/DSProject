#ifndef CLIENT
#define CLIENT

#include <QString>
#include <QThread>
#include <vector>
#include <iostream>
#include <QVariant>
#include <QNetworkInterface>
#include <QCoreApplication>
#include "qtxmlrpc.h"
#include "globals.h"

class Client : public XmlRpcClient{

	Q_OBJECT

public:

	enum State{
		FREE, REQUESTED, USING
	};

    Client();
	Client(QString host, int port);

	static QString nodeIp;
	static QString nodeIPnPort;
    static QStringList serverURLs;

	long EXECUTION_TIME;
	QList<QVariant> params;

    void setHost(QString host);

    bool isWaiting();

    void init();
    static void listOfNodes();

	void setHostAndPort(QString host_port);
	void setHostAndPort(QString host, int port);

	QString getHostAndPort();

signals:
	void finishedTask();

public slots:
    void join(QString newNodeIP);
    void signOff();
    void startElection();
    void startConcatProcess();
    void stopOperations();
    void runOverRpc(QString functionName, QList<QVariant> in_params);
    void showAllLists();
    void echo(QString newNodeIP,QString echoStr);
};

class ConcatBroadcaster : public QThread{

public:
	ConcatBroadcaster(QString serverURL);

	void run() Q_DECL_OVERRIDE;

private:
	QString serverURL;
};

#endif //CLIENT
