#ifndef SERVER
#define SERVER

#include <QString>
#include <QTcpServer>
#include <QCoreApplication>
#include <vector>
#include <iostream>
#include <QThread>
#include <ctime>
#include <iostream>
#include <fstream>
#include "qtxmlrpc.h"
#include "globals.h"
#include "globalclient.h"
#include "WordConcatenation.h"
#include "Bully.h"

class Server : public XmlRpcServer{

	Q_OBJECT

public:
	Server();

	void init();	

	static int stoppedNodes;

	public slots:
		QVariant join(const QVariant &newNodeIP);
		QVariant signOff(const QVariant &nodeIP);
		QVariant startElection(const QVariant &dummy);
		QVariant rpcElectionRequest(const QVariant &requester);
		QVariant hostBroadcast(const QVariant &newHost);
		QVariant startConcatProcess(const QVariant &dummy);
		QVariant rpcLifeSign(const QVariant &requester);
		QVariant checkConcatResult(const QVariant &dummy);
		QVariant echo(const QVariant& e) { return e; }
		QVariant rpcRequestString(const QVariant &dummy);
		QVariant rpcOverrideString(const QVariant &newString);

protected:
    std::vector<QString> rndWordSet;
	QString hostString;

	WordConcatenation* concatObject;
    std::vector<QString> requestQueue;
	bool critSectionBusy;
	bool isRunning;
	clock_t startTime;
    int stoppedRequester;

	void broadcastIamHost();
	bool concatLoop();
	bool loadFile();
	bool checkElapsedTime();
	void lockCritSection();
	void unlockCritSection();
	void broadCastCheckConcat();
};

#endif //SERVER
