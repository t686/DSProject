#ifndef SERVER
#define SERVER

#include <QString>
#include <QTcpServer>
#include <QCoreApplication>
#include <vector>
#include <iostream>
#include <QThread>
#include <ctime>
#include "qtxmlrpc.h"
#include "globals.h"
#include "Client.h"
#include "WordConcatenation.h"
#include "Bully.h"

class Server : public XmlRpcServer{

	Q_OBJECT

public:
	Server();

	void init();	

	static int stoppedNodes;

	Client* getClient();

	public slots:
		QVariant join(const QVariant &newNodeIP);
		QVariant signOff(const QVariant &nodeIP);
		QVariant startElection();
		QVariant rpcElectionRequest(const QVariant &requester);
		QVariant hostBroadcast(const QVariant &newHost);
		QVariant startConcatProcess();
		QVariant rpcLifeSign(const QVariant &requester);
		QVariant checkConcatResult();
		QVariant echo(const QVariant& e) { return e; }
		QVariant rpcRequestString();
		QVariant rpcOverrideString(const QVariant &newString);

protected:
	QString hostString;

	WordConcatenation* concatObject;
	QStringList requestQueue;
	bool critSectionBusy;
	bool isRunning;
    time_t startTime;
    int stoppedRequester;

	void broadcastIamHost();
	bool concatLoop();
	bool checkElapsedTime();
	void lockCritSection();
	void unlockCritSection();
	void broadCastCheckConcat();

	Client* xmlRpcClient;
};

#endif //SERVER
