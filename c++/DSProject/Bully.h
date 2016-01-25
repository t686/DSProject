#ifndef BULLY
#define BULLY

#include <QString>
#include <vector>
#include <QStringList>
#include <iostream>
#include <QVariant>
#include <QCoreApplication>
#include <QThread>
#include "globals.h"
#include "globalclient.h"

class Bully : public QObject{
public:
	Bully();

	static bool startElection(int ownPort, QList<QVariant> connectedNodes);

private:
    static QString messageNode(int ownPort, QString node);
	static bool signOffDisconnectedNode(QString node);
	static int extractPortFromIPnPort(QString nodeIPnPort);
};

#endif //BULLY
