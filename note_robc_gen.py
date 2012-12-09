filename = "tetris//tetris theme A notes.txt"
filename = "notes/tetris2.txt"
output_name = "tetris//tetris_top2.c"
task_name = "tetris_top"
title = "TETRIS TOP"



#length of a quarter note in milliseconds
length = 300

#ratio of frequency (ex: 0.5 cuts normal frequency of the notes by 1/2)
#messes around with sound
freq_ratio = 1

#waiting time between notes in milliseconds
wait_time = 0

#print type
#0 show nothing
#1 comments for notes
#2 print notes on screen
print_type = 2

#should I have spaces between notes?
spaces = False

#get note frequencies
ratio = 2**(1/12.0)
freq  = 27.5 * (ratio**-9)
notes = ['C', 'C+', 'D', 'D+', 'E', 'F', 'F+', 'G', 'G+', 'A', 'A+', 'B']
all_notes = []
all_freq  = []
note_freq = dict()

for num in range(8):
	for note in notes:
		all_notes.append(note+str(num))
		all_freq.append(int(round(freq)))
		freq *= ratio

note_freq = dict(zip(all_notes, all_freq))


def flat_to_sharp(note, octave):
        if note=='F':
                return 'E'+str(octave)
        elif note=='C':
                return 'B'+str(octave-1)
        else:
                d = {'B':'A', 'A':'G', 'G':'F', 'E':'D', 'D':'C'}
                return d[note]+'+'+str(octave)

for octave in range(8):
	for note in "ABCDEFG":
		note_sharp = flat_to_sharp(note, octave)
		note_freq[note+'-'+str(octave)] = note_freq.get(note_sharp, 0)



notes_file = open(filename, "r")
lines = notes_file.readlines()
lines = map(lambda s: s.strip('\n'), lines)
config_num = 0
#parse config
if lines[0] == "#config":
        for line in lines:
                config_num += 1
                if line == "#config":
                        continue
                if line == "#end":
                        break
                lis = line.strip(" ").strip("\n").split(": ")
                if lis[0] == 'Q':
                        length = (1.0/float(lis[1]))*60*1000
                elif lis[0] == 'T':
                        title = lis[1]



#parse notes
song = [] 
for row in lines[config_num+1:]:
	s = row.strip('\n').split(" ")
	if len(s) == 2:
		s[1] = float(s[1])
		song.append(s)
		#print s


output = open(output_name, "w")

#formalities
output.write("task " + task_name + "\n{\n")
output.write("""
	eraseDisplay();
	nxtDisplayCenteredBigTextLine(0, "{0}");
""".format(title))


length /= 10 #robotc takes time in 10MsecMachine

for note in song:
	if spaces:
		output.write("\n")

	if print_type == 1:
		output.write('\t//{0} {1}\n'.format(note[0], note[1]))
	elif print_type == 2:
		output.write('\tnxtDisplayTextLine(5, "{0} {1}");\n'.format(note[0], note[1]))
	

	if note[0] == "R":
		output.write("\twait10Msec({0});\n".format(length*note[1])) #rest
	else:
		#note
		output.write("\tPlayTone({0}, {1}); wait10Msec({1});\n" \
					.format(note_freq[note[0]]*freq_ratio, note[1]*length))
	
		if wait_time:
			output.write("\twait1Msec({0});\n".format(wait_time))


output.write("}")

print "Song generated"

output.close()
