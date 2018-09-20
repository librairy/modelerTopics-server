import numpy as np

betas = np.array([[0.3,0.2,0.5],[0.4,0.2,0.4],[0.3,0.6,0.1]])

size_vocab = betas.shape[1]
print(betas.T)

ntopics = betas.shape[0]
print(ntopics)

print("> 1")
print(betas)
print("> 2")
print(np.log(betas))
print("> 3")
print(sum(np.log(betas)))
print("> 4")
print(sum(np.log(betas))/ntopics)
#betas = np.array([[0.1,0.2]])
print("> 5")
print(np.reshape((sum(np.log(betas))/ntopics),(size_vocab,1)))
deno = np.reshape((sum(np.log(betas))/ntopics),(size_vocab,1))

print("> 6")
print(np.ones( (ntopics,1) ))
print("> 7")
print(np.ones( (ntopics,1) ).dot(deno.T))
print("> 8")
print(betas * (np.log(betas) - deno))





betas_ds = np.copy(betas)
if np.min(betas_ds) < 1e-12:
    betas_ds += 1e-12

deno = np.reshape((sum(np.log(betas_ds))/ntopics),(size_vocab,1))

deno = np.ones( (ntopics,1) ).dot(deno.T)

betas_ds = betas_ds * (np.log(betas_ds) - deno)

#print(betas_ds)